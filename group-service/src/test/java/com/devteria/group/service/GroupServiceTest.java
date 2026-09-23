package com.devteria.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.devteria.group.dto.GroupMemberRole;
import com.devteria.group.dto.request.GroupMemberRoleUpdateRequest;
import com.devteria.group.entity.Group;
import com.devteria.group.entity.GroupMember;
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

// Unit test thuần Mockito cho GroupService — tập trung vào 2 mảng logic phân quyền phức
// tạp nhất: role-permission matrix (requireManagePermission, qua changeMemberRole/removeMember)
// và ownership transfer — cả 2 đều đã được runtime-verify thủ công trước đây (docs/todo-be-group.md,
// docs/be-roadmap-hardening.md BE-6) nhưng chưa có test tự động nào khoá lại hành vi.
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    private static final String OWNER_ID = "user-owner";
    private static final String ADMIN_ID = "user-admin";
    private static final String MEMBER_ID = "user-member";
    private static final String GROUP_ID = "group-1";

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupPostRepository groupPostRepository;

    @Mock
    private GroupPostCommentRepository groupPostCommentRepository;

    @Mock
    private GroupPostLikeRepository groupPostLikeRepository;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private GroupMemberMapper groupMemberMapper;

    @Mock
    private ProfileClient profileClient;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private OutboxEventService outboxEventService;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(
                groupRepository,
                groupMemberRepository,
                groupPostRepository,
                groupPostCommentRepository,
                groupPostLikeRepository,
                groupMapper,
                groupMemberMapper,
                profileClient,
                mongoTemplate,
                outboxEventService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void actingAs(String userId) {
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(userId);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private GroupMember member(String userId, GroupMemberRole role) {
        return GroupMember.builder().groupId(GROUP_ID).userId(userId).role(role).build();
    }

    // ---- changeMemberRole() / requireManagePermission() ----

    @Test
    void changeMemberRole_ownerChangesMemberToAdmin_allowed() {
        actingAs(OWNER_ID);
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OWNER_ID))
                .thenReturn(Optional.of(member(OWNER_ID, GroupMemberRole.OWNER)));
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, MEMBER_ID))
                .thenReturn(Optional.of(member(MEMBER_ID, GroupMemberRole.MEMBER)));
        when(groupMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        groupService.changeMemberRole(
                GROUP_ID,
                MEMBER_ID,
                GroupMemberRoleUpdateRequest.builder()
                        .role(GroupMemberRole.ADMIN)
                        .build());

        ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(GroupMemberRole.ADMIN);
    }

    @Test
    void changeMemberRole_adminChangesMember_allowed() {
        actingAs(ADMIN_ID);
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, ADMIN_ID))
                .thenReturn(Optional.of(member(ADMIN_ID, GroupMemberRole.ADMIN)));
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, MEMBER_ID))
                .thenReturn(Optional.of(member(MEMBER_ID, GroupMemberRole.MEMBER)));
        when(groupMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        groupService.changeMemberRole(
                GROUP_ID,
                MEMBER_ID,
                GroupMemberRoleUpdateRequest.builder()
                        .role(GroupMemberRole.ADMIN)
                        .build());

        verify(groupMemberRepository).save(any());
    }

    @Test
    void changeMemberRole_adminChangesAnotherAdmin_forbidden() {
        actingAs(ADMIN_ID);
        String otherAdminId = "user-admin-2";
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, ADMIN_ID))
                .thenReturn(Optional.of(member(ADMIN_ID, GroupMemberRole.ADMIN)));
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, otherAdminId))
                .thenReturn(Optional.of(member(otherAdminId, GroupMemberRole.ADMIN)));

        var exception = assertThrows(
                AppException.class,
                () -> groupService.changeMemberRole(
                        GROUP_ID,
                        otherAdminId,
                        GroupMemberRoleUpdateRequest.builder()
                                .role(GroupMemberRole.MEMBER)
                                .build()));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_GROUP_PERMISSION);
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void changeMemberRole_memberTriesToChangeAnother_forbidden() {
        actingAs(MEMBER_ID);
        String otherMemberId = "user-member-2";
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, MEMBER_ID))
                .thenReturn(Optional.of(member(MEMBER_ID, GroupMemberRole.MEMBER)));
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, otherMemberId))
                .thenReturn(Optional.of(member(otherMemberId, GroupMemberRole.MEMBER)));

        var exception = assertThrows(
                AppException.class,
                () -> groupService.changeMemberRole(
                        GROUP_ID,
                        otherMemberId,
                        GroupMemberRoleUpdateRequest.builder()
                                .role(GroupMemberRole.ADMIN)
                                .build()));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_GROUP_PERMISSION);
    }

    @Test
    void changeMemberRole_targetIsOwner_cannotChangeOwnerRole() {
        actingAs(ADMIN_ID);
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, ADMIN_ID))
                .thenReturn(Optional.of(member(ADMIN_ID, GroupMemberRole.ADMIN)));
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OWNER_ID))
                .thenReturn(Optional.of(member(OWNER_ID, GroupMemberRole.OWNER)));

        var exception = assertThrows(
                AppException.class,
                () -> groupService.changeMemberRole(
                        GROUP_ID,
                        OWNER_ID,
                        GroupMemberRoleUpdateRequest.builder()
                                .role(GroupMemberRole.MEMBER)
                                .build()));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_CHANGE_OWNER_ROLE);
    }

    // ---- transferOwnership() ----

    @Test
    void transferOwnership_callerNotOwner_forbidden() {
        actingAs(ADMIN_ID);
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, ADMIN_ID))
                .thenReturn(Optional.of(member(ADMIN_ID, GroupMemberRole.ADMIN)));

        var exception = assertThrows(AppException.class, () -> groupService.transferOwnership(GROUP_ID, MEMBER_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_GROUP_PERMISSION);
    }

    @Test
    void transferOwnership_toSelf_throwsCannotTransferToSelf() {
        actingAs(OWNER_ID);
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OWNER_ID))
                .thenReturn(Optional.of(member(OWNER_ID, GroupMemberRole.OWNER)));

        var exception = assertThrows(AppException.class, () -> groupService.transferOwnership(GROUP_ID, OWNER_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_TRANSFER_TO_SELF);
    }

    @Test
    void transferOwnership_targetNotMember_throwsMemberNotFound() {
        actingAs(OWNER_ID);
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OWNER_ID))
                .thenReturn(Optional.of(member(OWNER_ID, GroupMemberRole.OWNER)));
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(AppException.class, () -> groupService.transferOwnership(GROUP_ID, MEMBER_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GROUP_MEMBER_NOT_FOUND);
    }

    @Test
    void transferOwnership_valid_swapsRolesAndUpdatesGroupOwnerId() {
        actingAs(OWNER_ID);
        GroupMember caller = member(OWNER_ID, GroupMemberRole.OWNER);
        GroupMember target = member(MEMBER_ID, GroupMemberRole.MEMBER);
        Group group = Group.builder().id(GROUP_ID).ownerId(OWNER_ID).build();

        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OWNER_ID)).thenReturn(Optional.of(caller));
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(target));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        groupService.transferOwnership(GROUP_ID, MEMBER_ID);

        assertThat(caller.getRole()).isEqualTo(GroupMemberRole.ADMIN);
        assertThat(target.getRole()).isEqualTo(GroupMemberRole.OWNER);

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());
        assertThat(groupCaptor.getValue().getOwnerId()).isEqualTo(MEMBER_ID);

        verify(groupMemberRepository, times(2)).save(any());
    }

    // ---- transferOwnershipOnOwnerLockedBySystem() ----

    @Test
    void transferOwnershipOnOwnerLocked_picksEarliestJoinedAdminOverMember() {
        GroupMember owner = member(OWNER_ID, GroupMemberRole.OWNER);
        GroupMember lateAdmin = GroupMember.builder()
                .groupId(GROUP_ID)
                .userId("admin-late")
                .role(GroupMemberRole.ADMIN)
                .joinedAt(java.time.Instant.parse("2026-02-01T00:00:00Z"))
                .build();
        GroupMember earlyAdmin = GroupMember.builder()
                .groupId(GROUP_ID)
                .userId("admin-early")
                .role(GroupMemberRole.ADMIN)
                .joinedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        GroupMember earlierMember = GroupMember.builder()
                .groupId(GROUP_ID)
                .userId("member-earliest")
                .role(GroupMemberRole.MEMBER)
                .joinedAt(java.time.Instant.parse("2025-01-01T00:00:00Z"))
                .build();
        Group group = Group.builder().id(GROUP_ID).ownerId(OWNER_ID).build();

        when(groupMemberRepository.findByUserId(OWNER_ID)).thenReturn(java.util.List.of(owner));
        when(groupMemberRepository.findByGroupId(GROUP_ID))
                .thenReturn(java.util.List.of(owner, lateAdmin, earlyAdmin, earlierMember));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        groupService.transferOwnershipOnOwnerLockedBySystem(OWNER_ID);

        assertThat(owner.getRole()).isEqualTo(GroupMemberRole.ADMIN);
        assertThat(earlyAdmin.getRole()).isEqualTo(GroupMemberRole.OWNER);
        assertThat(lateAdmin.getRole()).isEqualTo(GroupMemberRole.ADMIN);
        assertThat(group.getOwnerId()).isEqualTo("admin-early");
    }

    @Test
    void transferOwnershipOnOwnerLocked_noAdmin_fallsBackToEarliestMember() {
        GroupMember owner = member(OWNER_ID, GroupMemberRole.OWNER);
        GroupMember lateMember = GroupMember.builder()
                .groupId(GROUP_ID)
                .userId("member-late")
                .role(GroupMemberRole.MEMBER)
                .joinedAt(java.time.Instant.parse("2026-02-01T00:00:00Z"))
                .build();
        GroupMember earlyMember = GroupMember.builder()
                .groupId(GROUP_ID)
                .userId("member-early")
                .role(GroupMemberRole.MEMBER)
                .joinedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        Group group = Group.builder().id(GROUP_ID).ownerId(OWNER_ID).build();

        when(groupMemberRepository.findByUserId(OWNER_ID)).thenReturn(java.util.List.of(owner));
        when(groupMemberRepository.findByGroupId(GROUP_ID))
                .thenReturn(java.util.List.of(owner, lateMember, earlyMember));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        groupService.transferOwnershipOnOwnerLockedBySystem(OWNER_ID);

        assertThat(earlyMember.getRole()).isEqualTo(GroupMemberRole.OWNER);
        assertThat(group.getOwnerId()).isEqualTo("member-early");
    }

    @Test
    void transferOwnershipOnOwnerLocked_soleMember_noOpsNoSave() {
        GroupMember owner = member(OWNER_ID, GroupMemberRole.OWNER);

        when(groupMemberRepository.findByUserId(OWNER_ID)).thenReturn(java.util.List.of(owner));
        when(groupMemberRepository.findByGroupId(GROUP_ID)).thenReturn(java.util.List.of(owner));

        groupService.transferOwnershipOnOwnerLockedBySystem(OWNER_ID);

        verify(groupMemberRepository, never()).save(any());
        verify(groupRepository, never()).save(any());
    }

    // ---- leaveGroup() ----

    @Test
    void leaveGroup_owner_throwsOwnerCannotLeave() {
        actingAs(OWNER_ID);
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OWNER_ID))
                .thenReturn(Optional.of(member(OWNER_ID, GroupMemberRole.OWNER)));

        var exception = assertThrows(AppException.class, () -> groupService.leaveGroup(GROUP_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GROUP_OWNER_CANNOT_LEAVE);
        verify(groupMemberRepository, never()).deleteByGroupIdAndUserId(any(), any());
    }

    @Test
    void leaveGroup_ownerAfterTransfer_nowAdmin_canLeave() {
        // Đúng luồng "hết kẹt nhóm" sau ownership transfer: OWNER cũ giờ là ADMIN, rời được bình thường.
        actingAs(OWNER_ID);
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OWNER_ID))
                .thenReturn(Optional.of(member(OWNER_ID, GroupMemberRole.ADMIN)));
        when(groupMemberRepository.findByGroupId(GROUP_ID)).thenReturn(java.util.List.of());

        groupService.leaveGroup(GROUP_ID);

        verify(groupMemberRepository).deleteByGroupIdAndUserId(GROUP_ID, OWNER_ID);
        verify(outboxEventService).recordEvent(eq(GROUP_ID), any(), any());
    }

    @Test
    void leaveGroup_notMember_throwsNotGroupMember() {
        actingAs(MEMBER_ID);
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(AppException.class, () -> groupService.leaveGroup(GROUP_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_GROUP_MEMBER);
    }

    // ---- joinGroup() ----

    @Test
    void joinGroup_alreadyMember_throwsAlreadyGroupMember() {
        actingAs(MEMBER_ID);
        Group group = Group.builder().id(GROUP_ID).name("Test Group").build();
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, MEMBER_ID))
                .thenReturn(true);

        var exception = assertThrows(AppException.class, () -> groupService.joinGroup(GROUP_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_GROUP_MEMBER);
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void joinGroup_valid_savesMemberWithMemberRole() {
        actingAs(MEMBER_ID);
        Group group = Group.builder().id(GROUP_ID).name("Test Group").build();
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, MEMBER_ID))
                .thenReturn(false);
        when(groupMemberRepository.findByGroupId(GROUP_ID)).thenReturn(java.util.List.of());
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.empty());
        when(groupMemberRepository.countByGroupId(GROUP_ID)).thenReturn(0L);

        groupService.joinGroup(GROUP_ID);

        ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(GroupMemberRole.MEMBER);
        assertThat(captor.getValue().getUserId()).isEqualTo(MEMBER_ID);
        verify(outboxEventService).recordEvent(eq(GROUP_ID), any(), any());
    }

    // ---- updateGroupStatus() (BA v2 §2.3) ----
    // @PreAuthorize không được enforce trong unit test thuần (không có Spring AOP proxy) - test
    // này chỉ verify hành vi set status thật, không verify chặn quyền (đã có @PreAuthorize khai
    // báo, tin tưởng cơ chế chung của Spring Security giống mọi @PreAuthorize khác trong dự án).

    @Test
    void updateGroupStatus_setsStatusOnGroup() {
        actingAs("admin-1");
        Group group = Group.builder()
                .id(GROUP_ID)
                .status(com.devteria.group.dto.GroupStatus.ACTIVE)
                .build();
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.countByGroupId(GROUP_ID)).thenReturn(0L);
        when(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, "admin-1")).thenReturn(Optional.empty());

        groupService.updateGroupStatus(GROUP_ID, com.devteria.group.dto.GroupStatus.FROZEN);

        assertThat(group.getStatus()).isEqualTo(com.devteria.group.dto.GroupStatus.FROZEN);
        verify(groupRepository).save(group);
    }
}
