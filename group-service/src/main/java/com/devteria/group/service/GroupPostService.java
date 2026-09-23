package com.devteria.group.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.group.dto.GroupEventPayload;
import com.devteria.group.dto.GroupMemberRole;
import com.devteria.group.dto.GroupPostStatus;
import com.devteria.group.dto.OutboxEventType;
import com.devteria.group.dto.request.GroupPostCommentCreateRequest;
import com.devteria.group.dto.request.GroupPostCreateRequest;
import com.devteria.group.dto.request.GroupPostRejectRequest;
import com.devteria.group.dto.response.ApiResponse;
import com.devteria.group.dto.response.BookLookupResponse;
import com.devteria.group.dto.response.GroupPostCommentResponse;
import com.devteria.group.dto.response.GroupPostResponse;
import com.devteria.group.dto.response.UserProfileResponse;
import com.devteria.group.entity.BookRef;
import com.devteria.group.entity.Group;
import com.devteria.group.entity.GroupMember;
import com.devteria.group.entity.GroupPost;
import com.devteria.group.entity.GroupPostComment;
import com.devteria.group.exception.AppException;
import com.devteria.group.exception.ErrorCode;
import com.devteria.group.mapper.GroupPostCommentMapper;
import com.devteria.group.mapper.GroupPostMapper;
import com.devteria.group.repository.GroupMemberRepository;
import com.devteria.group.repository.GroupPostCommentRepository;
import com.devteria.group.repository.GroupPostLikeRepository;
import com.devteria.group.repository.GroupPostRepository;
import com.devteria.group.repository.httpclient.BookClient;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupPostService {
    GroupService groupService;
    GroupPostRepository groupPostRepository;
    GroupPostCommentRepository groupPostCommentRepository;
    GroupPostLikeRepository groupPostLikeRepository;
    GroupPostMapper groupPostMapper;
    GroupPostCommentMapper groupPostCommentMapper;
    GroupMemberRepository groupMemberRepository;
    OutboxEventService outboxEventService;
    BookClient bookClient;

    public Page<GroupPostResponse> listPosts(String groupId, int page, int size) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        Group group = groupService.getGroupOrThrow(groupId);
        groupService.requireVisibleContentAccess(group, currentUserId);

        Pageable pageable = PageRequest.of(page, size);
        // idea-spec BA OPS-01: feed công khai chỉ hiện bài đã PUBLISHED (coi bài cũ thiếu field
        // status là PUBLISHED, xem ghi chú ở GroupPostRepository.findPublishedByGroupId) — bài
        // PENDING_APPROVAL/REJECTED nằm ở hàng chờ duyệt riêng (xem listPendingPosts()).
        Page<GroupPost> posts = groupPostRepository.findPublishedByGroupId(groupId, pageable);

        return posts.map(post -> toGroupPostResponse(post, currentUserId));
    }

    // idea-spec BA OPS-01
    public Page<GroupPostResponse> listPendingPosts(String groupId, int page, int size) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        requireGroupManager(groupId, currentUserId);

        Pageable pageable = PageRequest.of(page, size);
        Page<GroupPost> posts = groupPostRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(
                groupId, GroupPostStatus.PENDING_APPROVAL, pageable);

        return posts.map(post -> toGroupPostResponse(post, currentUserId));
    }

    public GroupPostResponse createPost(String groupId, GroupPostCreateRequest request) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        groupService.requireMembership(groupId, currentUserId);
        Group group = groupService.getGroupOrThrow(groupId);

        // idea-spec BA v2 §2.3: nhóm FROZEN chỉ đọc - không cho đăng bài mới. ADMIN vẫn qua được
        // (Ultimate Authority §2.2, cùng nguyên tắc admin bypass đã áp dụng cho các hành động khác).
        if (group.getStatus() == com.devteria.group.dto.GroupStatus.FROZEN && !groupService.isAdmin()) {
            throw new AppException(ErrorCode.GROUP_FROZEN);
        }

        BookRef bookRef = null;
        if (request.getBookId() != null && !request.getBookId().isBlank()) {
            ApiResponse<BookLookupResponse> response;
            try {
                response = bookClient.getBookById(request.getBookId());
            } catch (FeignException.NotFound e) {
                throw new AppException(ErrorCode.BOOK_NOT_FOUND);
            }
            if (response == null || response.getResult() == null) {
                throw new AppException(ErrorCode.BOOK_NOT_FOUND);
            }
            var book = response.getResult();
            bookRef = BookRef.builder()
                    .bookId(book.getId())
                    .bookTitle(book.getTitle())
                    .authorName(
                            book.getAuthors() != null && !book.getAuthors().isEmpty()
                                    ? book.getAuthors().getFirst().getName()
                                    : null)
                    .coverImage(book.getMetadata() != null ? book.getMetadata().getCoverImage() : null)
                    .build();
        }

        // idea-spec BA OPS-01: nhóm bật requireApproval -> bài mới vào hàng chờ duyệt, kể cả
        // OWNER/ADMIN tự đăng (đọc đúng theo văn bản spec, không loại trừ theo role tác giả).
        GroupPostStatus status =
                group.isRequireApproval() ? GroupPostStatus.PENDING_APPROVAL : GroupPostStatus.PUBLISHED;

        GroupPost post = GroupPost.builder()
                .groupId(groupId)
                .authorId(currentUserId)
                .content(request.getContent())
                .bookRef(bookRef)
                .status(status)
                .createdAt(Instant.now())
                .build();
        post = groupPostRepository.save(post);

        if (status == GroupPostStatus.PUBLISHED) {
            outboxEventService.recordEvent(
                    groupId,
                    OutboxEventType.GROUP_POST_CREATED,
                    GroupEventPayload.builder()
                            .groupId(groupId)
                            .postId(post.getId())
                            .actorId(currentUserId)
                            .targetUserIds(otherMemberIds(groupId, currentUserId))
                            .build());
        }

        return toGroupPostResponse(post, currentUserId);
    }

    // idea-spec BA OPS-01
    public GroupPostResponse approvePost(String groupId, String postId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        requireGroupManager(groupId, currentUserId);
        GroupPost post = getPendingPostOrThrow(groupId, postId);

        post.setStatus(GroupPostStatus.PUBLISHED);
        post = groupPostRepository.save(post);

        outboxEventService.recordEvent(
                groupId,
                OutboxEventType.GROUP_POST_CREATED,
                GroupEventPayload.builder()
                        .groupId(groupId)
                        .postId(post.getId())
                        .actorId(post.getAuthorId())
                        .targetUserIds(otherMemberIds(groupId, post.getAuthorId()))
                        .build());

        return toGroupPostResponse(post, currentUserId);
    }

    // idea-spec BA OPS-01
    public void rejectPost(String groupId, String postId, GroupPostRejectRequest request) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        requireGroupManager(groupId, currentUserId);
        GroupPost post = getPendingPostOrThrow(groupId, postId);

        post.setStatus(GroupPostStatus.REJECTED);
        post.setRejectionReason(request != null ? request.getReason() : null);
        groupPostRepository.save(post);
    }

    private GroupPost getPendingPostOrThrow(String groupId, String postId) {
        GroupPost post = getPostOrThrow(postId);
        if (!post.getGroupId().equals(groupId)) {
            throw new AppException(ErrorCode.GROUP_POST_NOT_FOUND);
        }
        if (post.getStatus() != GroupPostStatus.PENDING_APPROVAL) {
            throw new AppException(ErrorCode.GROUP_POST_NOT_PENDING);
        }
        return post;
    }

    public void toggleLike(String groupId, String postId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        groupService.requireMembership(groupId, currentUserId);
        getPostOrThrow(postId);

        if (groupPostLikeRepository.existsByPostIdAndUserId(postId, currentUserId)) {
            groupPostLikeRepository.deleteByPostIdAndUserId(postId, currentUserId);
        } else {
            groupPostLikeRepository.save(com.devteria.group.entity.GroupPostLike.builder()
                    .postId(postId)
                    .userId(currentUserId)
                    .createdAt(Instant.now())
                    .build());
        }
    }

    public GroupPostCommentResponse addComment(String groupId, String postId, GroupPostCommentCreateRequest request) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        groupService.requireMembership(groupId, currentUserId);
        getPostOrThrow(postId);

        GroupPostComment comment = GroupPostComment.builder()
                .postId(postId)
                .authorId(currentUserId)
                .content(request.getContent())
                .createdAt(Instant.now())
                .build();
        comment = groupPostCommentRepository.save(comment);

        outboxEventService.recordEvent(
                groupId,
                OutboxEventType.GROUP_COMMENT_CREATED,
                GroupEventPayload.builder()
                        .groupId(groupId)
                        .postId(postId)
                        .commentId(comment.getId())
                        .actorId(currentUserId)
                        .targetUserIds(otherMemberIds(groupId, currentUserId))
                        .build());

        UserProfileResponse profile = groupService.getProfile(currentUserId);
        return groupPostCommentMapper.toGroupPostCommentResponse(comment, profile);
    }

    public void deletePost(String groupId, String postId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        GroupPost post = getPostOrThrow(postId);
        requireAuthorOrManager(groupId, currentUserId, post.getAuthorId());

        groupPostRepository.delete(post);

        outboxEventService.recordEvent(
                groupId,
                OutboxEventType.GROUP_POST_DELETED,
                GroupEventPayload.builder()
                        .groupId(groupId)
                        .postId(postId)
                        .actorId(currentUserId)
                        .targetUserIds(otherMemberIds(groupId, currentUserId))
                        .build());
    }

    public void deleteComment(String groupId, String postId, String commentId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        GroupPostComment comment = groupPostCommentRepository
                .findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_POST_COMMENT_NOT_FOUND));
        requireAuthorOrManager(groupId, currentUserId, comment.getAuthorId());

        groupPostCommentRepository.delete(comment);
    }

    private void requireAuthorOrManager(String groupId, String currentUserId, String authorId) {
        if (currentUserId.equals(authorId)) {
            return;
        }
        requireGroupManager(groupId, currentUserId);
    }

    // idea-spec Phase 4 - "13. Group Moderation" / BA OPS-01: OWNER/ADMIN của group, hoặc
    // platform ADMIN kể cả không phải member của group đó.
    private void requireGroupManager(String groupId, String currentUserId) {
        if (groupService.isAdmin()) {
            return;
        }
        GroupMember caller = groupService.requireMembership(groupId, currentUserId);
        if (caller.getRole() != GroupMemberRole.OWNER && caller.getRole() != GroupMemberRole.ADMIN) {
            throw new AppException(ErrorCode.INSUFFICIENT_GROUP_PERMISSION);
        }
    }

    private List<String> otherMemberIds(String groupId, String excludeUserId) {
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .filter(userId -> !userId.equals(excludeUserId))
                .toList();
    }

    private GroupPost getPostOrThrow(String postId) {
        return groupPostRepository.findById(postId).orElseThrow(() -> new AppException(ErrorCode.GROUP_POST_NOT_FOUND));
    }

    private GroupPostResponse toGroupPostResponse(GroupPost post, String currentUserId) {
        long likesCount = groupPostLikeRepository.countByPostId(post.getId());
        long commentsCount = groupPostCommentRepository.countByPostId(post.getId());
        boolean liked = groupPostLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        List<GroupPostComment> commentEntities =
                groupPostCommentRepository.findByPostIdOrderByCreatedAtAsc(post.getId());

        // Batch 1 lần cho tác giả bài viết + tất cả tác giả bình luận — thay vì 1+N lần getProfile() riêng lẻ.
        List<String> authorIds = new ArrayList<>();
        authorIds.add(post.getAuthorId());
        commentEntities.forEach(comment -> authorIds.add(comment.getAuthorId()));
        Map<String, UserProfileResponse> profiles = groupService.getProfilesByIds(authorIds);

        UserProfileResponse profile = groupService.requireProfile(profiles, post.getAuthorId());
        List<GroupPostCommentResponse> comments = commentEntities.stream()
                .map(comment -> groupPostCommentMapper.toGroupPostCommentResponse(
                        comment, groupService.requireProfile(profiles, comment.getAuthorId())))
                .toList();

        return groupPostMapper.toGroupPostResponse(post, profile, likesCount, commentsCount, liked, comments);
    }
}
