package com.devteria.group.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.devteria.group.dto.GroupEventPayload;
import com.devteria.group.dto.GroupIndexEventPayload;
import com.devteria.group.dto.GroupMemberRole;
import com.devteria.group.dto.GroupStatus;
import com.devteria.group.dto.OutboxEventType;
import com.devteria.group.dto.request.GroupCreateRequest;
import com.devteria.group.dto.request.GroupMemberRoleUpdateRequest;
import com.devteria.group.dto.request.GroupSettingsUpdateRequest;
import com.devteria.group.dto.request.UserIdsRequest;
import com.devteria.group.dto.response.GroupMemberResponse;
import com.devteria.group.dto.response.GroupResponse;
import com.devteria.group.dto.response.UserProfileResponse;
import com.devteria.group.entity.Group;
import com.devteria.group.entity.GroupMember;
import com.devteria.group.entity.GroupPost;
import com.devteria.group.exception.AppException;
import com.devteria.group.exception.ErrorCode;
import com.devteria.group.mapper.GroupMapper;
import com.devteria.group.mapper.GroupMemberMapper;
import com.devteria.group.repository.GroupMemberRepository;
import com.devteria.group.repository.GroupPostCommentRepository;
import com.devteria.group.repository.GroupPostLikeRepository;
import com.devteria.group.repository.GroupPostRepository;
import com.devteria.group.repository.GroupRepository;
import com.devteria.group.repository.httpclient.ProfileClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GroupService {
    GroupRepository groupRepository;
    GroupMemberRepository groupMemberRepository;
    GroupPostRepository groupPostRepository;
    GroupPostCommentRepository groupPostCommentRepository;
    GroupPostLikeRepository groupPostLikeRepository;
    GroupMapper groupMapper;
    GroupMemberMapper groupMemberMapper;
    ProfileClient profileClient;
    MongoTemplate mongoTemplate;
    OutboxEventService outboxEventService;

    public GroupResponse createGroup(GroupCreateRequest request) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .avatar(request.getAvatar())
                .coverImage(request.getCoverImage())
                .visibility(request.getVisibility())
                .category(request.getCategory())
                .ownerId(currentUserId)
                .rules(request.getRules())
                .requireApproval(Boolean.TRUE.equals(request.getRequireApproval()))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        group = groupRepository.save(group);

        GroupMember owner = GroupMember.builder()
                .groupId(group.getId())
                .userId(currentUserId)
                .role(GroupMemberRole.OWNER)
                .joinedAt(Instant.now())
                .build();
        groupMemberRepository.save(owner);

        // idea-spec Phase 6 - "22.1 Search Integration"
        outboxEventService.recordEvent(
                group.getId(),
                OutboxEventType.GROUP_CREATED,
                GroupIndexEventPayload.builder()
                        .groupId(group.getId())
                        .name(group.getName())
                        .description(group.getDescription())
                        .category(group.getCategory())
                        .visibility(
                                group.getVisibility() != null
                                        ? group.getVisibility().name()
                                        : "PUBLIC")
                        .createdAt(group.getCreatedAt())
                        .build());

        return toGroupResponse(group, currentUserId);
    }

    public Page<GroupResponse> listGroups(String search, String category, int page, int size) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        Query query = new Query();
        // idea-spec BA GAP-03/BA v2 §2.3: nhóm SUSPENDED không hiện trong khám phá công khai —
        // owner/member vẫn truy cập được qua getMyGroups()/getGroupDetail() (không lọc ở đó).
        // FROZEN vẫn hiện bình thường (chỉ chặn đăng bài mới, không phải ẩn khỏi discovery).
        query.addCriteria(Criteria.where("status").ne(GroupStatus.SUSPENDED));
        if (StringUtils.hasText(search)) {
            query.addCriteria(Criteria.where("name").regex(search, "i"));
        }
        if (StringUtils.hasText(category)) {
            query.addCriteria(Criteria.where("category").is(category));
        }

        long total = mongoTemplate.count(query, Group.class);
        Pageable pageable = PageRequest.of(page, size);
        query.with(pageable);
        List<Group> groups = mongoTemplate.find(query, Group.class);

        List<GroupResponse> content = groups.stream()
                .map(group -> toGroupResponse(group, currentUserId))
                .toList();

        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }

    public List<GroupResponse> getMyGroups() {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        List<String> groupIds = groupMemberRepository.findByUserId(currentUserId).stream()
                .map(GroupMember::getGroupId)
                .toList();

        return groupRepository.findByIdIn(groupIds).stream()
                .map(group -> toGroupResponse(group, currentUserId))
                .toList();
    }

    public GroupResponse getGroupDetail(String groupId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        Group group = getGroupOrThrow(groupId);
        return toGroupResponse(group, currentUserId);
    }

    public GroupResponse joinGroup(String groupId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        Group group = getGroupOrThrow(groupId);

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)) {
            throw new AppException(ErrorCode.ALREADY_GROUP_MEMBER);
        }

        GroupMember member = GroupMember.builder()
                .groupId(groupId)
                .userId(currentUserId)
                .role(GroupMemberRole.MEMBER)
                .joinedAt(Instant.now())
                .build();
        groupMemberRepository.save(member);

        outboxEventService.recordEvent(
                groupId,
                OutboxEventType.GROUP_MEMBER_JOINED,
                GroupEventPayload.builder()
                        .groupId(groupId)
                        .groupName(group.getName())
                        .userId(currentUserId)
                        .targetUserIds(otherMemberIds(groupId, currentUserId))
                        .build());

        return toGroupResponse(group, currentUserId);
    }

    public void leaveGroup(String groupId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        GroupMember member = groupMemberRepository
                .findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_GROUP_MEMBER));

        if (member.getRole() == GroupMemberRole.OWNER) {
            throw new AppException(ErrorCode.GROUP_OWNER_CANNOT_LEAVE);
        }

        List<String> targetUserIds = otherMemberIds(groupId, currentUserId);
        groupMemberRepository.deleteByGroupIdAndUserId(groupId, currentUserId);

        outboxEventService.recordEvent(
                groupId,
                OutboxEventType.GROUP_MEMBER_LEFT,
                GroupEventPayload.builder()
                        .groupId(groupId)
                        .userId(currentUserId)
                        .targetUserIds(targetUserIds)
                        .build());
    }

    public List<GroupMemberResponse> getMembers(String groupId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        Group group = getGroupOrThrow(groupId);
        requireVisibleContentAccess(group, currentUserId);

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        Map<String, UserProfileResponse> profiles =
                getProfilesByIds(members.stream().map(GroupMember::getUserId).toList());

        return members.stream()
                .map(member ->
                        groupMemberMapper.toGroupMemberResponse(member, requireProfile(profiles, member.getUserId())))
                .toList();
    }

    public void changeMemberRole(String groupId, String targetUserId, GroupMemberRoleUpdateRequest request) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        GroupMember target = requireManagePermission(groupId, targetUserId);
        target.setRole(request.getRole());
        groupMemberRepository.save(target);

        outboxEventService.recordEvent(
                groupId,
                OutboxEventType.GROUP_ROLE_CHANGED,
                GroupEventPayload.builder()
                        .groupId(groupId)
                        .userId(targetUserId)
                        .actorId(currentUserId)
                        .role(target.getRole())
                        .targetUserIds(List.of(targetUserId))
                        .build());
    }

    private List<String> otherMemberIds(String groupId, String excludeUserId) {
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .filter(userId -> !userId.equals(excludeUserId))
                .toList();
    }

    public void removeMember(String groupId, String targetUserId) {
        // idea-spec Phase 4 - "13. Group Moderation": platform ADMIN gỡ member vi phạm được
        // kể cả khi ADMIN đó không phải member của group này (requireManagePermission đòi caller
        // phải là member - bypass ở đây).
        if (isAdmin()) {
            GroupMember target = groupMemberRepository
                    .findByGroupIdAndUserId(groupId, targetUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));
            groupMemberRepository.deleteByGroupIdAndUserId(groupId, target.getUserId());
            log.info(
                    "[AUDIT] admin={} action=REMOVE_MEMBER target=group:{} member={}",
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    groupId,
                    targetUserId);
            return;
        }
        GroupMember target = requireManagePermission(groupId, targetUserId);
        groupMemberRepository.deleteByGroupIdAndUserId(groupId, target.getUserId());
    }

    // idea-spec Phase 4 - "13. Group Moderation": group OWNER hoặc platform ADMIN xoá group vi
    // phạm. Cascade xoá member/post/comment/like liên quan - tránh mồ côi.
    public void deleteGroup(String groupId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        Group group = getGroupOrThrow(groupId);
        if (!group.getOwnerId().equals(currentUserId) && !isAdmin()) {
            throw new AppException(ErrorCode.INSUFFICIENT_GROUP_PERMISSION);
        }
        if (!group.getOwnerId().equals(currentUserId)) {
            log.info(
                    "[AUDIT] admin={} action=DELETE_GROUP target=group:{} owner={}",
                    currentUserId,
                    groupId,
                    group.getOwnerId());
        }

        List<String> postIds = groupPostRepository.findByGroupId(groupId).stream()
                .map(GroupPost::getId)
                .toList();
        if (!postIds.isEmpty()) {
            groupPostCommentRepository.deleteAllByPostIdIn(postIds);
            groupPostLikeRepository.deleteAllByPostIdIn(postIds);
        }
        groupPostRepository.deleteAllByGroupId(groupId);
        groupMemberRepository.deleteAllByGroupId(groupId);
        groupRepository.delete(group);

        // idea-spec Phase 6 - "22.1 Search Integration"
        outboxEventService.recordEvent(groupId, OutboxEventType.GROUP_DELETED, java.util.Map.of("groupId", groupId));
    }

    // Chuyển quyền OWNER cho 1 thành viên khác — chỉ OWNER hiện tại gọi được.
    // Sau khi chuyển, OWNER cũ tự động thành ADMIN (không rời nhóm "kẹt" nữa vì
    // leaveGroup() chặn OWNER rời — giờ họ không còn là OWNER nên rời được bình
    // thường). Group.ownerId (denormalized) cũng được cập nhật theo.
    public void transferOwnership(String groupId, String newOwnerUserId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        GroupMember caller = requireMembership(groupId, currentUserId);
        if (caller.getRole() != GroupMemberRole.OWNER) {
            throw new AppException(ErrorCode.INSUFFICIENT_GROUP_PERMISSION);
        }
        if (currentUserId.equals(newOwnerUserId)) {
            throw new AppException(ErrorCode.CANNOT_TRANSFER_TO_SELF);
        }

        GroupMember newOwner = groupMemberRepository
                .findByGroupIdAndUserId(groupId, newOwnerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        caller.setRole(GroupMemberRole.ADMIN);
        newOwner.setRole(GroupMemberRole.OWNER);
        groupMemberRepository.save(caller);
        groupMemberRepository.save(newOwner);

        Group group = getGroupOrThrow(groupId);
        group.setOwnerId(newOwnerUserId);
        group.setUpdatedAt(Instant.now());
        groupRepository.save(group);
    }

    // idea-spec BA OPS-01: "Tự động Chuyển giao Quyền Quản trị" khi OWNER bị Admin hệ thống khoá
    // (USER_LOCKED, xem UserLockEventConsumer) — chọn ADMIN có joinedAt sớm nhất; nếu nhóm không
    // còn ADMIN nào khác, fallback MEMBER có joinedAt sớm nhất; nếu owner là thành viên duy nhất,
    // không có ai để chuyển - giữ nguyên, chỉ log cảnh báo (edge case hiếm). Gọi từ Kafka consumer
    // (không có SecurityContext) nên không tái dùng transferOwnership() gốc.
    public void transferOwnershipOnOwnerLockedBySystem(String lockedUserId) {
        List<GroupMember> ownedGroups = groupMemberRepository.findByUserId(lockedUserId).stream()
                .filter(member -> member.getRole() == GroupMemberRole.OWNER)
                .toList();

        for (GroupMember ownerMembership : ownedGroups) {
            String groupId = ownerMembership.getGroupId();
            List<GroupMember> otherMembers = groupMemberRepository.findByGroupId(groupId).stream()
                    .filter(member -> !member.getUserId().equals(lockedUserId))
                    .toList();

            GroupMember successor = otherMembers.stream()
                    .filter(member -> member.getRole() == GroupMemberRole.ADMIN)
                    .min(Comparator.comparing(GroupMember::getJoinedAt))
                    .or(() -> otherMembers.stream().min(Comparator.comparing(GroupMember::getJoinedAt)))
                    .orElse(null);

            if (successor == null) {
                log.warn(
                        "Group {} có OWNER {} bị khoá nhưng không còn thành viên nào khác để tự động chuyển quyền.",
                        groupId,
                        lockedUserId);
                continue;
            }

            ownerMembership.setRole(GroupMemberRole.ADMIN);
            successor.setRole(GroupMemberRole.OWNER);
            groupMemberRepository.save(ownerMembership);
            groupMemberRepository.save(successor);

            Group group = getGroupOrThrow(groupId);
            group.setOwnerId(successor.getUserId());
            group.setUpdatedAt(Instant.now());
            groupRepository.save(group);

            log.info(
                    "[AUDIT] source=identity-service action=USER_LOCKED effect=AUTO_TRANSFER_OWNERSHIP group={} from={} to={}",
                    groupId,
                    lockedUserId,
                    successor.getUserId());
        }
    }

    // idea-spec BA OPS-01: OWNER/ADMIN bật/tắt duyệt bài viết cho nhóm đã tồn tại.
    public void updateGroupSettings(String groupId, GroupSettingsUpdateRequest request) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        Group group = getGroupOrThrow(groupId);

        if (!isAdmin()) {
            GroupMember caller = requireMembership(groupId, currentUserId);
            if (caller.getRole() != GroupMemberRole.OWNER && caller.getRole() != GroupMemberRole.ADMIN) {
                throw new AppException(ErrorCode.INSUFFICIENT_GROUP_PERMISSION);
            }
        }

        group.setRequireApproval(Boolean.TRUE.equals(request.getRequireApproval()));
        group.setUpdatedAt(Instant.now());
        groupRepository.save(group);
    }

    // idea-spec Phase 6 - "20. Popular Groups": kết hợp memberCount (nhóm đông) + số bài đăng
    // gần đây (nhóm hoạt động cao, 30 ngày gần nhất). Tính trong Java trên toàn bộ danh sách
    // group (hoặc theo category nếu lọc) - cùng lý do như book-service's Trending Books, không
    // dùng Mongo aggregation pipeline vì không có Mongo thật để verify cú pháp trong phiên code
    // này. Chấp nhận được ở quy mô hiện tại.
    private static final double POPULAR_WEIGHT_MEMBER = 1.0;
    private static final double POPULAR_WEIGHT_RECENT_ACTIVITY = 3.0;
    private static final int RECENT_ACTIVITY_DAYS = 30;

    public Page<GroupResponse> getPopularGroups(String category, int page, int size) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        List<Group> candidates = ((category != null && !category.isBlank())
                        ? groupRepository.findByCategory(category)
                        : groupRepository.findAll())
                .stream()
                        .filter(group -> group.getStatus() != GroupStatus.SUSPENDED) // idea-spec BA GAP-03/BA v2 §2.3
                        .toList();

        java.time.Instant since =
                java.time.Instant.now().minus(RECENT_ACTIVITY_DAYS, java.time.temporal.ChronoUnit.DAYS);

        List<Group> sorted = candidates.stream()
                .sorted(java.util.Comparator.comparingDouble((Group group) -> popularityScore(group, since))
                        .reversed())
                .toList();

        int totalElements = sorted.size();
        int from = Math.min(page * size, totalElements);
        int to = Math.min(from + size, totalElements);
        List<GroupResponse> content = sorted.subList(from, to).stream()
                .map(group -> toGroupResponse(group, currentUserId))
                .toList();

        Pageable pageable = PageRequest.of(page, size);
        return new org.springframework.data.domain.PageImpl<>(content, pageable, totalElements);
    }

    private double popularityScore(Group group, java.time.Instant since) {
        long memberCount = groupMemberRepository.countByGroupId(group.getId());
        long recentPosts = groupPostRepository.countByGroupIdAndCreatedAtAfter(group.getId(), since);
        return memberCount * POPULAR_WEIGHT_MEMBER + recentPosts * POPULAR_WEIGHT_RECENT_ACTIVITY;
    }

    // ---- helpers, package-private so GroupPostService can reuse them ----

    Group getGroupOrThrow(String groupId) {
        return groupRepository.findById(groupId).orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
    }

    void requireVisibleContentAccess(Group group, String currentUserId) {
        // idea-spec Phase 4 - "13. Group Moderation": platform ADMIN xem được nội dung group
        // PRIVATE (members/posts) kể cả không phải member, để điều tra report.
        if (isAdmin()) {
            return;
        }
        boolean memberOnly = group.getVisibility() == com.devteria.group.dto.GroupVisibility.PRIVATE
                || group.getStatus()
                        == GroupStatus.SUSPENDED; // idea-spec BA GAP-03: SUSPENDED xử lý như PRIVATE với non-member
        if (memberOnly && !groupMemberRepository.existsByGroupIdAndUserId(group.getId(), currentUserId)) {
            throw new AppException(ErrorCode.NOT_GROUP_MEMBER);
        }
    }

    // idea-spec BA GAP-03: thực thi lệnh HIDE_GROUP từ ADMIN_COMMAND_EXECUTE (report-service) —
    // gọi từ Kafka consumer, không có SecurityContext nên không tái dùng flow REST thường.
    public void hideGroupBySystem(String groupId) {
        groupRepository.findById(groupId).ifPresent(group -> {
            group.setStatus(GroupStatus.SUSPENDED);
            group.setUpdatedAt(Instant.now());
            groupRepository.save(group);
        });
    }

    // idea-spec BA v2 §2.3: "Thay đổi trạng thái Nhóm: ACTIVE, FROZEN, SUSPENDED" — Admin đổi trực
    // tiếp qua REST (khác hideGroupBySystem() vốn chỉ gọi từ report-service qua Kafka).
    @PreAuthorize("hasRole('ADMIN')")
    public GroupResponse updateGroupStatus(String groupId, GroupStatus status) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        Group group = getGroupOrThrow(groupId);
        group.setStatus(status);
        group.setUpdatedAt(Instant.now());
        group = groupRepository.save(group);

        log.info(
                "[AUDIT] admin={} action=UPDATE_GROUP_STATUS target=group:{} status={}",
                currentUserId,
                groupId,
                status);

        return toGroupResponse(group, currentUserId);
    }

    // idea-spec BA v2 §2.3: "Giao diện Quản lý Toàn bộ Nhóm... tìm kiếm, lọc và xem chi tiết tất
    // cả các Nhóm (cả Public và Private)" — khác listGroups() (discovery công khai, luôn loại
    // SUSPENDED), endpoint này cho ADMIN xem TẤT CẢ kể cả SUSPENDED, lọc theo status nếu cần.
    @PreAuthorize("hasRole('ADMIN')")
    public Page<GroupResponse> listGroupsForAdmin(
            String search, String category, GroupStatus status, int page, int size) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        Query query = new Query();
        if (StringUtils.hasText(search)) {
            query.addCriteria(Criteria.where("name").regex(search, "i"));
        }
        if (StringUtils.hasText(category)) {
            query.addCriteria(Criteria.where("category").is(category));
        }
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        long total = mongoTemplate.count(query, Group.class);
        Pageable pageable = PageRequest.of(page, size);
        query.with(pageable);
        List<Group> groups = mongoTemplate.find(query, Group.class);

        List<GroupResponse> content = groups.stream()
                .map(group -> toGroupResponse(group, currentUserId))
                .toList();

        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }

    boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    GroupMember requireMembership(String groupId, String userId) {
        return groupMemberRepository
                .findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_GROUP_MEMBER));
    }

    UserProfileResponse getProfile(String userId) {
        var response = profileClient.getProfile(userId);
        if (response == null || response.getResult() == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return response.getResult();
    }

    // Batch fetch 1 lần thay vì N lần getProfile() riêng lẻ — chống N+1 khi liệt kê nhiều member/tác giả.
    Map<String, UserProfileResponse> getProfilesByIds(List<String> userIds) {
        List<String> distinctIds = userIds.stream().distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        var response = profileClient.getProfiles(
                UserIdsRequest.builder().userIds(distinctIds).build());
        List<UserProfileResponse> profiles =
                (response == null || response.getResult() == null) ? List.of() : response.getResult();
        return profiles.stream().collect(Collectors.toMap(UserProfileResponse::getUserId, p -> p, (a, b) -> a));
    }

    UserProfileResponse requireProfile(Map<String, UserProfileResponse> profiles, String userId) {
        UserProfileResponse profile = profiles.get(userId);
        if (profile == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return profile;
    }

    private GroupMember requireManagePermission(String groupId, String targetUserId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        GroupMember caller = requireMembership(groupId, currentUserId);
        GroupMember target = groupMemberRepository
                .findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (target.getRole() == GroupMemberRole.OWNER) {
            throw new AppException(ErrorCode.CANNOT_CHANGE_OWNER_ROLE);
        }

        boolean allowed = caller.getRole() == GroupMemberRole.OWNER
                || (caller.getRole() == GroupMemberRole.ADMIN && target.getRole() == GroupMemberRole.MEMBER);
        if (!allowed) {
            throw new AppException(ErrorCode.INSUFFICIENT_GROUP_PERMISSION);
        }

        return target;
    }

    private GroupResponse toGroupResponse(Group group, String currentUserId) {
        long memberCount = groupMemberRepository.countByGroupId(group.getId());
        GroupMember membership = groupMemberRepository
                .findByGroupIdAndUserId(group.getId(), currentUserId)
                .orElse(null);
        boolean joined = Objects.nonNull(membership);
        GroupMemberRole currentUserRole = joined ? membership.getRole() : null;

        return groupMapper.toGroupResponse(group, memberCount, joined, currentUserRole);
    }
}
