package com.devteria.post.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.post.dto.OutboxEventType;
import com.devteria.post.dto.PostIndexEventPayload;
import com.devteria.post.dto.PostMentionEventPayload;
import com.devteria.post.dto.PostVisibility;
import com.devteria.post.dto.request.CommentRequest;
import com.devteria.post.dto.request.PostRequest;
import com.devteria.post.dto.request.ShareRequest;
import com.devteria.post.dto.response.BookLookupResponse;
import com.devteria.post.dto.response.CommentResponse;
import com.devteria.post.dto.response.PageResponse;
import com.devteria.post.dto.response.PostResponse;
import com.devteria.post.dto.response.UserProfileResponse;
import com.devteria.post.entity.Post;
import com.devteria.post.entity.PostComment;
import com.devteria.post.entity.PostLike;
import com.devteria.post.exception.AppException;
import com.devteria.post.exception.ErrorCode;
import com.devteria.post.mapper.PostMapper;
import com.devteria.post.repository.PostCommentRepository;
import com.devteria.post.repository.PostLikeRepository;
import com.devteria.post.repository.PostRepository;
import com.devteria.post.repository.httpclient.BookClient;
import com.devteria.post.repository.httpclient.FriendClient;
import com.devteria.post.repository.httpclient.ProfileClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PostService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

    PostRepository postRepository;
    PostLikeRepository postLikeRepository;
    PostCommentRepository postCommentRepository;
    PostMapper postMapper;
    DateTimeFormatter dateTimeFormatter;
    ProfileClient profileClient;
    FriendClient friendClient;
    BookClient bookClient;
    OutboxEventService outboxEventService;

    public PostResponse createPost(PostRequest request) {
        String currentUserId = currentUserId();

        if (request.getBookId() != null && !request.getBookId().isBlank()) {
            validateBookExists(request.getBookId());
        }

        List<String> hashtags = extractHashtags(request.getContent());
        List<String> mentionedUserIds = resolveMentionedUserIds(request.getContent());

        PostVisibility visibility = request.getVisibility() != null ? request.getVisibility() : PostVisibility.PUBLIC;

        Post post = Post.builder()
                .userId(currentUserId)
                .content(request.getContent())
                .visibility(visibility)
                .bookId(request.getBookId())
                .mediaUrls(request.getMediaUrls())
                .hashtags(hashtags)
                .mentionedUserIds(mentionedUserIds)
                .likesCount(0)
                .commentsCount(0)
                .createdDate(Instant.now())
                .modifiedDate(Instant.now())
                .build();
        postRepository.save(post);

        // idea-spec Phase 6 - "22.1 Search Integration"
        outboxEventService.recordEvent(
                post.getId(),
                OutboxEventType.POST_CREATED,
                PostIndexEventPayload.builder()
                        .postId(post.getId())
                        .content(post.getContent())
                        .authorId(post.getUserId())
                        .hashtags(post.getHashtags())
                        .bookId(post.getBookId())
                        .visibility(post.getVisibility().name())
                        .createdDate(post.getCreatedDate())
                        .build());

        if (!mentionedUserIds.isEmpty()) {
            UserProfileResponse authorProfile = getProfileSafe(currentUserId);
            outboxEventService.recordEvent(
                    post.getId(),
                    OutboxEventType.POST_MENTIONED,
                    PostMentionEventPayload.builder()
                            .postId(post.getId())
                            .authorId(currentUserId)
                            .authorUsername(authorProfile != null ? authorProfile.getUsername() : null)
                            .mentionedUserIds(mentionedUserIds)
                            .build());
        }

        return toEnrichedResponse(post, currentUserId);
    }

    public PageResponse<PostResponse> getMyPosts(int page, int size) {
        String currentUserId = currentUserId();
        Pageable pageable = PageRequest.of(page, size);
        var pageData = postRepository.findAllByUserId(currentUserId, pageable);
        return toPageResponse(pageData, currentUserId, page, size);
    }

    // idea-spec Phase 3 - "5. Global Feed": xem feed cộng đồng (mọi post PUBLIC).
    public PageResponse<PostResponse> getGlobalFeed(int page, int size) {
        String viewerId = currentUserId();
        Pageable pageable = PageRequest.of(page, size);
        var pageData = postRepository.findAllByVisibilityOrderByCreatedDateDesc(PostVisibility.PUBLIC, pageable);
        return toPageResponse(pageData, viewerId, page, size);
    }

    // idea-spec Phase 3 - "5. Global Feed" (feed theo bạn bè).
    public PageResponse<PostResponse> getFriendFeed(int page, int size) {
        String viewerId = currentUserId();
        List<String> friendIds = getFriendIdsSafe(viewerId);
        if (friendIds.isEmpty()) {
            return PageResponse.<PostResponse>builder()
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages(0)
                    .totalElements(0)
                    .data(List.of())
                    .build();
        }
        Pageable pageable = PageRequest.of(page, size);
        var pageData = postRepository.findAllByUserIdInAndVisibilityInOrderByCreatedDateDesc(
                friendIds, List.of(PostVisibility.PUBLIC, PostVisibility.FRIENDS), pageable);
        return toPageResponse(pageData, viewerId, page, size);
    }

    // idea-spec Phase 6 - "22. Personalized Feed": gộp 2 tín hiệu - bạn bè (tái dùng đúng logic
    // getFriendFeed) và bài viết PUBLIC gắn với sách trong reading-list của chính user (tín hiệu
    // "quan tâm tới sách"). Merge + dedupe + sort lại theo thời gian trong Java, phân trang thủ
    // công - lấy tối đa 50 bài mỗi nguồn trước khi merge, đủ cho 1 trang feed cá nhân hoá, tránh
    // phải kéo toàn bộ dữ liệu.
    // KHÔNG bao gồm §22.1 Search Integration (mở rộng search-service/Elasticsearch cho Post/
    // Hashtag/Group) - quyết định phạm vi có chủ đích, xem be-report.md Known Issues: cần
    // Elasticsearch thật đang chạy để phát triển/verify mapping mới an toàn, không có trong môi
    // trường phiên code này, và là 1 khối việc đủ lớn (index mapping mới + consumer Kafka mới ở
    // search-service + query DSL mới) xứng đáng 1 phiên riêng.
    private static final int PERSONALIZED_FEED_SOURCE_LIMIT = 50;

    public PageResponse<PostResponse> getPersonalizedFeed(int page, int size) {
        String viewerId = currentUserId();

        List<Post> combined = new ArrayList<>();
        List<String> friendIds = getFriendIdsSafe(viewerId);
        if (!friendIds.isEmpty()) {
            combined.addAll(postRepository
                    .findAllByUserIdInAndVisibilityInOrderByCreatedDateDesc(
                            friendIds,
                            List.of(PostVisibility.PUBLIC, PostVisibility.FRIENDS),
                            PageRequest.of(0, PERSONALIZED_FEED_SOURCE_LIMIT))
                    .getContent());
        }

        List<String> myBookIds = getMyReadingListBookIdsSafe();
        if (!myBookIds.isEmpty()) {
            combined.addAll(postRepository
                    .findAllByBookIdInAndVisibilityOrderByCreatedDateDesc(
                            myBookIds, PostVisibility.PUBLIC, PageRequest.of(0, PERSONALIZED_FEED_SOURCE_LIMIT))
                    .getContent());
        }

        List<Post> deduped = combined.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Post::getId, post -> post, (a, b) -> a, java.util.LinkedHashMap::new))
                .values()
                .stream()
                .sorted(java.util.Comparator.comparing(Post::getCreatedDate).reversed())
                .toList();

        int totalElements = deduped.size();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int from = Math.min(page * size, totalElements);
        int to = Math.min(from + size, totalElements);
        List<PostResponse> content = deduped.subList(from, to).stream()
                .map(post -> toEnrichedResponse(post, viewerId))
                .toList();

        return PageResponse.<PostResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .data(content)
                .build();
    }

    private List<String> getMyReadingListBookIdsSafe() {
        try {
            var response = bookClient.getMyReadingList(0, 200);
            if (response == null
                    || response.getResult() == null
                    || response.getResult().getData() == null) {
                return List.of();
            }
            return response.getResult().getData().stream()
                    .map(com.devteria.post.dto.response.ReadingListItemResponse::getBookId)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("Could not fetch reading list for personalized feed, viewerId={}", currentUserId(), e);
            return List.of();
        }
    }

    // Xem bài viết của 1 user cụ thể - tự lọc visibility theo quan hệ với viewer
    // (idea-spec Phase 3 - "8. Post Visibility").
    public PageResponse<PostResponse> getUserPosts(String targetUserId, int page, int size) {
        String viewerId = currentUserId();
        List<PostVisibility> visibilities;
        if (viewerId.equals(targetUserId)) {
            visibilities = List.of(PostVisibility.PUBLIC, PostVisibility.FRIENDS, PostVisibility.PRIVATE);
        } else if (isFriend(targetUserId, viewerId)) {
            visibilities = List.of(PostVisibility.PUBLIC, PostVisibility.FRIENDS);
        } else {
            visibilities = List.of(PostVisibility.PUBLIC);
        }
        Pageable pageable = PageRequest.of(page, size);
        var pageData = postRepository.findAllByUserIdAndVisibilityInOrderByCreatedDateDesc(
                targetUserId, visibilities, pageable);
        return toPageResponse(pageData, viewerId, page, size);
    }

    public PostResponse getPost(String postId) {
        String viewerId = currentUserId();
        Post post = getViewablePostOrThrow(postId, viewerId);
        return toEnrichedResponse(post, viewerId);
    }

    // idea-spec Phase 3 - "6. Post Like": toggle - like nếu chưa like, unlike nếu đã like.
    public PostResponse toggleLike(String postId) {
        String viewerId = currentUserId();
        Post post = getViewablePostOrThrow(postId, viewerId);

        if (postLikeRepository.existsByPostIdAndUserId(postId, viewerId)) {
            postLikeRepository.deleteByPostIdAndUserId(postId, viewerId);
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
        } else {
            try {
                postLikeRepository.save(PostLike.builder()
                        .postId(postId)
                        .userId(viewerId)
                        .createdAt(Instant.now())
                        .build());
                post.setLikesCount(post.getLikesCount() + 1);
            } catch (DuplicateKeyException e) {
                // 2 request like gần như đồng thời - unique index (postId, userId) chặn ở DB,
                // coi như đã like rồi, không tăng đếm lần 2.
            }
        }
        postRepository.save(post);
        return toEnrichedResponse(post, viewerId);
    }

    // idea-spec Phase 3 - "7. Post Comment"
    public CommentResponse addComment(String postId, CommentRequest request) {
        String viewerId = currentUserId();
        Post post = getViewablePostOrThrow(postId, viewerId);

        PostComment comment = PostComment.builder()
                .postId(postId)
                .authorId(viewerId)
                .content(request.getContent())
                .createdAt(Instant.now())
                .build();
        postCommentRepository.save(comment);

        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        return toCommentResponse(comment);
    }

    public PageResponse<CommentResponse> getComments(String postId, int page, int size) {
        String viewerId = currentUserId();
        getViewablePostOrThrow(postId, viewerId);

        Pageable pageable = PageRequest.of(page, size);
        var pageData = postCommentRepository.findAllByPostIdOrderByCreatedAtDesc(postId, pageable);
        var data = pageData.getContent().stream().map(this::toCommentResponse).toList();

        return PageResponse.<CommentResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(data)
                .build();
    }

    public CommentResponse updateComment(String commentId, CommentRequest request) {
        String viewerId = currentUserId();
        PostComment comment = postCommentRepository
                .findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getAuthorId().equals(viewerId)) {
            throw new AppException(ErrorCode.COMMENT_ACCESS_DENIED);
        }
        comment.setContent(request.getContent());
        comment.setUpdatedAt(Instant.now());
        postCommentRepository.save(comment);
        return toCommentResponse(comment);
    }

    public void deleteComment(String commentId) {
        String viewerId = currentUserId();
        PostComment comment = postCommentRepository
                .findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getAuthorId().equals(viewerId)) {
            throw new AppException(ErrorCode.COMMENT_ACCESS_DENIED);
        }
        postCommentRepository.deleteById(commentId);

        postRepository.findById(comment.getPostId()).ifPresent(post -> {
            post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
            postRepository.save(post);
        });
    }

    // idea-spec BA GAP-03: xoá comment do lệnh ADMIN_COMMAND_EXECUTE (report-service, sau khi
    // Admin duyệt report ACTION_TAKEN) — gọi từ Kafka consumer, không có SecurityContext/JWT nào
    // trong luồng đó nên không thể tái dùng deleteComment() (đọc currentUserId() sẽ NPE). Quyền
    // ADMIN đã được xác nhận thật ở report-service lúc Admin PATCH status (@PreAuthorize ở đó),
    // đây chỉ là thực thi lệnh đã được duyệt, không check lại quyền lần 2.
    public void deleteCommentBySystem(String commentId) {
        PostComment comment = postCommentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return;
        }
        postCommentRepository.deleteById(commentId);
        postRepository.findById(comment.getPostId()).ifPresent(post -> {
            post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
            postRepository.save(post);
        });
    }

    // idea-spec Phase 4 - "12. Post Moderation": tác giả tự xoá post của mình, hoặc platform
    // ADMIN xoá post vi phạm của người khác. Cascade xoá like/comment liên quan - tránh mồ côi.
    public void deletePost(String postId) {
        String currentUserId = currentUserId();
        Post post = postRepository.findById(postId).orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        if (!post.getUserId().equals(currentUserId) && !isAdmin()) {
            throw new AppException(ErrorCode.POST_ACCESS_DENIED);
        }
        if (!post.getUserId().equals(currentUserId)) {
            log.info(
                    "[AUDIT] admin={} action=DELETE_POST target=post:{} owner={}",
                    currentUserId,
                    postId,
                    post.getUserId());
        }
        postLikeRepository.deleteAllByPostId(postId);
        postCommentRepository.deleteAllByPostId(postId);
        postRepository.delete(post);

        // idea-spec Phase 6 - "22.1 Search Integration"
        outboxEventService.recordEvent(postId, OutboxEventType.POST_DELETED, Map.of("postId", postId));
    }

    // idea-spec BA GAP-03: tương tự deleteCommentBySystem() — thực thi lệnh DELETE_POST đã được
    // report-service duyệt qua Kafka, không có SecurityContext để tái dùng deletePost().
    public void deletePostBySystem(String postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return;
        }
        postLikeRepository.deleteAllByPostId(postId);
        postCommentRepository.deleteAllByPostId(postId);
        postRepository.delete(post);
        outboxEventService.recordEvent(postId, OutboxEventType.POST_DELETED, Map.of("postId", postId));
    }

    // idea-spec Phase 3 - "10.2 Share / Re-post"
    public PostResponse sharePost(String postId, ShareRequest request) {
        String viewerId = currentUserId();
        Post original = getViewablePostOrThrow(postId, viewerId);

        Post repost = Post.builder()
                .userId(viewerId)
                .content(request.getContent())
                .visibility(PostVisibility.PUBLIC)
                .sharedFromPostId(original.getId())
                .likesCount(0)
                .commentsCount(0)
                .createdDate(Instant.now())
                .modifiedDate(Instant.now())
                .build();
        postRepository.save(repost);
        return toEnrichedResponse(repost, viewerId);
    }

    // ==================== Helper ====================

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private Post getViewablePostOrThrow(String postId, String viewerId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        if (!canView(post, viewerId)) {
            throw new AppException(ErrorCode.POST_ACCESS_DENIED);
        }
        return post;
    }

    private boolean canView(Post post, String viewerId) {
        if (post.getUserId().equals(viewerId)) {
            return true;
        }
        // idea-spec Phase 4 - "12. Post Moderation": platform ADMIN xem được mọi post bất kể
        // visibility, để điều tra report.
        if (isAdmin()) {
            return true;
        }
        PostVisibility visibility = post.getVisibility() != null ? post.getVisibility() : PostVisibility.PUBLIC;
        if (visibility == PostVisibility.PUBLIC) {
            return true;
        }
        if (visibility == PostVisibility.PRIVATE) {
            return false;
        }
        return isFriend(post.getUserId(), viewerId);
    }

    private boolean isFriend(String userId, String otherUserId) {
        List<String> friendIds = getFriendIdsSafe(userId);
        return friendIds.contains(otherUserId);
    }

    private List<String> getFriendIdsSafe(String userId) {
        try {
            var response = friendClient.getFriendUserIds(userId);
            if (response != null && response.getResult() != null) {
                return response.getResult();
            }
        } catch (Exception e) {
            log.error("Could not fetch friend ids for userId={}", userId, e);
        }
        return List.of();
    }

    private void validateBookExists(String bookId) {
        try {
            var response = bookClient.getBookById(bookId);
            if (response == null || response.getResult() == null) {
                throw new AppException(ErrorCode.BOOK_NOT_FOUND);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.BOOK_NOT_FOUND);
        }
    }

    private BookLookupResponse getBookSafe(String bookId) {
        try {
            var response = bookClient.getBookById(bookId);
            return response != null ? response.getResult() : null;
        } catch (Exception e) {
            log.warn("Could not fetch book info for bookId={}", bookId, e);
            return null;
        }
    }

    private UserProfileResponse getProfileSafe(String userId) {
        try {
            var response = profileClient.getProfile(userId);
            return response != null ? response.getResult() : null;
        } catch (Exception e) {
            log.error("Error while fetching user profile", e);
            return null;
        }
    }

    private List<String> extractHashtags(String content) {
        if (content == null) {
            return List.of();
        }
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        List<String> tags = new ArrayList<>();
        while (matcher.find()) {
            tags.add(matcher.group(1));
        }
        return tags.stream().distinct().toList();
    }

    private List<String> resolveMentionedUserIds(String content) {
        if (content == null) {
            return List.of();
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        List<String> userIds = new ArrayList<>();
        while (matcher.find()) {
            String username = matcher.group(1);
            try {
                var response = profileClient.getProfileByUsername(username);
                if (response != null
                        && response.getResult() != null
                        && response.getResult().getUserId() != null) {
                    userIds.add(response.getResult().getUserId());
                }
            } catch (Exception e) {
                log.debug("Mentioned username not found, skip: {}", username);
            }
        }
        return userIds.stream().distinct().toList();
    }

    private PageResponse<PostResponse> toPageResponse(Page<Post> pageData, String viewerId, int page, int size) {
        var data = pageData.getContent().stream()
                .map(post -> toEnrichedResponse(post, viewerId))
                .toList();
        return PageResponse.<PostResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(data)
                .build();
    }

    private PostResponse toEnrichedResponse(Post post, String viewerId) {
        PostResponse response = postMapper.toPostResponse(post);
        response.setCreated(dateTimeFormatter.format(post.getCreatedDate()));
        response.setVisibility(post.getVisibility() != null ? post.getVisibility() : PostVisibility.PUBLIC);

        UserProfileResponse profile = getProfileSafe(post.getUserId());
        response.setUsername(profile != null ? profile.getUsername() : null);

        if (post.getBookId() != null && !post.getBookId().isBlank()) {
            BookLookupResponse book = getBookSafe(post.getBookId());
            if (book != null) {
                response.setBookTitle(book.getTitle());
                response.setBookCoverImage(
                        book.getMetadata() != null ? book.getMetadata().getCoverImage() : null);
            }
        }

        response.setLiked(postLikeRepository.existsByPostIdAndUserId(post.getId(), viewerId));
        return response;
    }

    private CommentResponse toCommentResponse(PostComment comment) {
        UserProfileResponse profile = getProfileSafe(comment.getAuthorId());
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .authorUsername(profile != null ? profile.getUsername() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
