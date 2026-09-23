package com.devteria.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.devteria.group.dto.GroupStatus;
import com.devteria.group.dto.request.GroupPostCreateRequest;
import com.devteria.group.entity.Group;
import com.devteria.group.exception.AppException;
import com.devteria.group.exception.ErrorCode;
import com.devteria.group.mapper.GroupPostCommentMapper;
import com.devteria.group.mapper.GroupPostMapper;
import com.devteria.group.repository.GroupMemberRepository;
import com.devteria.group.repository.GroupPostCommentRepository;
import com.devteria.group.repository.GroupPostLikeRepository;
import com.devteria.group.repository.GroupPostRepository;
import com.devteria.group.repository.httpclient.BookClient;

// Unit test thuần Mockito cho GroupPostService - GroupService là mock (không phải test thật
// GroupService, chỉ cần đúng hành vi requireMembership()/getGroupOrThrow()/isAdmin() được gọi
// đúng). Tập trung vào FROZEN gate (BA v2 §2.3, code mới) - GroupPostService trước đó 0 test.
@ExtendWith(MockitoExtension.class)
class GroupPostServiceTest {

    private static final String USER_ID = "user-1";
    private static final String GROUP_ID = "group-1";

    @Mock
    private GroupService groupService;

    @Mock
    private GroupPostRepository groupPostRepository;

    @Mock
    private GroupPostCommentRepository groupPostCommentRepository;

    @Mock
    private GroupPostLikeRepository groupPostLikeRepository;

    @Mock
    private GroupPostMapper groupPostMapper;

    @Mock
    private GroupPostCommentMapper groupPostCommentMapper;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private BookClient bookClient;

    private GroupPostService groupPostService;

    @BeforeEach
    void setUp() {
        groupPostService = new GroupPostService(
                groupService,
                groupPostRepository,
                groupPostCommentRepository,
                groupPostLikeRepository,
                groupPostMapper,
                groupPostCommentMapper,
                groupMemberRepository,
                outboxEventService,
                bookClient);

        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(USER_ID);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPost_frozenGroup_nonAdmin_throwsGroupFrozen() {
        Group frozenGroup =
                Group.builder().id(GROUP_ID).status(GroupStatus.FROZEN).build();
        when(groupService.getGroupOrThrow(GROUP_ID)).thenReturn(frozenGroup);
        when(groupService.isAdmin()).thenReturn(false);

        var exception = assertThrows(
                AppException.class,
                () -> groupPostService.createPost(
                        GROUP_ID,
                        GroupPostCreateRequest.builder().content("hello").build()));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GROUP_FROZEN);
    }

    @Test
    void createPost_activeGroup_nonAdmin_doesNotThrowGroupFrozen() {
        Group activeGroup =
                Group.builder().id(GROUP_ID).status(GroupStatus.ACTIVE).build();
        when(groupService.getGroupOrThrow(GROUP_ID)).thenReturn(activeGroup);

        // Không stub đủ để hoàn thành toàn bộ createPost() (mapper/response) - chỉ cần xác nhận
        // exception (nếu có) không phải do FROZEN gate, tức là gate không chặn nhầm nhóm ACTIVE.
        try {
            groupPostService.createPost(
                    GROUP_ID, GroupPostCreateRequest.builder().content("hello").build());
        } catch (AppException e) {
            assertThat(e.getErrorCode()).isNotEqualTo(ErrorCode.GROUP_FROZEN);
        } catch (Exception ignored) {
            // NPE từ mock chưa stub đủ (mapper trả null...) - chấp nhận được, không phải điều test này quan tâm.
        }
    }
}
