package com.devteria.friend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.devteria.friend.dto.FriendRequestStatus;
import com.devteria.friend.dto.request.FriendRequestCreateRequest;
import com.devteria.friend.dto.response.ApiResponse;
import com.devteria.friend.dto.response.FriendRequestResponse;
import com.devteria.friend.dto.response.UserProfileResponse;
import com.devteria.friend.entity.Block;
import com.devteria.friend.entity.FriendRequest;
import com.devteria.friend.exception.AppException;
import com.devteria.friend.exception.ErrorCode;
import com.devteria.friend.mapper.BlockMapper;
import com.devteria.friend.mapper.FriendMapper;
import com.devteria.friend.repository.BlockRepository;
import com.devteria.friend.repository.FriendRequestRepository;
import com.devteria.friend.repository.httpclient.ProfileClient;

// Unit test thuần Mockito (không Spring context) cho FriendService — tập trung vào
// logic dễ vỡ nhất đã từng có bug thật trong phiên làm việc trước: operator-precedence
// NPE ở cancelFriendRelationship(), race-condition qua DuplicateKeyException, và
// block-check 2 chiều ở sendRequest().
@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    private static final String CURRENT_USER_ID = "user-A";
    private static final String OTHER_USER_ID = "user-B";

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private ProfileClient profileClient;

    @Mock
    private FriendMapper friendMapper;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private BlockMapper blockMapper;

    private FriendService friendService;

    @BeforeEach
    void setUp() {
        friendService = new FriendService(
                friendRequestRepository, profileClient, friendMapper, outboxEventService, blockRepository, blockMapper);

        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(CURRENT_USER_ID);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- blockUser() ----

    @Test
    void blockUser_self_throwsCannotBlockYourself() {
        var exception = assertThrows(AppException.class, () -> friendService.blockUser(CURRENT_USER_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_BLOCK_YOURSELF);
        verify(blockRepository, never()).save(any());
    }

    @Test
    void blockUser_alreadyBlocked_throwsAlreadyBlocked() {
        stubProfileExists(OTHER_USER_ID);
        when(blockRepository.existsByBlockerIdAndBlockedId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(true);

        var exception = assertThrows(AppException.class, () -> friendService.blockUser(OTHER_USER_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_BLOCKED);
        verify(blockRepository, never()).save(any());
    }

    @Test
    void blockUser_duplicateKeyRace_throwsAlreadyBlocked() {
        // 2 request block gần như đồng thời cùng lọt qua existsBy() -> unique index chặn ở DB.
        stubProfileExists(OTHER_USER_ID);
        when(blockRepository.existsByBlockerIdAndBlockedId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(false);
        when(blockRepository.save(any())).thenThrow(new DuplicateKeyException("duplicate"));

        var exception = assertThrows(AppException.class, () -> friendService.blockUser(OTHER_USER_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_BLOCKED);
    }

    // Regression test cho bug thật đã fix trong phiên trước: operator-precedence khiến
    // cancelFriendRelationship() NPE khi KHÔNG có friend request nào giữa 2 người (trường
    // hợp phổ biến nhất — block 1 người xa lạ). Test này phải chạy không throw.
    @Test
    void blockUser_noExistingFriendRequest_doesNotThrow() {
        stubProfileExists(OTHER_USER_ID);
        when(blockRepository.existsByBlockerIdAndBlockedId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(false);
        when(blockRepository.save(any())).thenReturn(Block.builder().build());
        when(friendRequestRepository.findBySenderIdAndReceiverId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findBySenderIdAndReceiverId(OTHER_USER_ID, CURRENT_USER_ID))
                .thenReturn(Optional.empty());

        assertThatCode(() -> friendService.blockUser(OTHER_USER_ID)).doesNotThrowAnyException();

        verify(friendRequestRepository, never()).save(any());
        verify(outboxEventService).recordEvent(eq(CURRENT_USER_ID), any(), any());
    }

    @Test
    void blockUser_cancelsPendingRequestBothDirections() {
        stubProfileExists(OTHER_USER_ID);
        when(blockRepository.existsByBlockerIdAndBlockedId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(false);
        when(blockRepository.save(any())).thenReturn(Block.builder().build());

        FriendRequest requestAtoB = FriendRequest.builder()
                .id("req-1")
                .senderId(CURRENT_USER_ID)
                .receiverId(OTHER_USER_ID)
                .status(FriendRequestStatus.PENDING)
                .build();
        when(friendRequestRepository.findBySenderIdAndReceiverId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(Optional.of(requestAtoB));
        when(friendRequestRepository.findBySenderIdAndReceiverId(OTHER_USER_ID, CURRENT_USER_ID))
                .thenReturn(Optional.empty());

        friendService.blockUser(OTHER_USER_ID);

        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(FriendRequestStatus.CANCELLED);
    }

    // ---- sendRequest() ----

    @Test
    void sendRequest_blockedByReceiver_throwsBlocked() {
        FriendRequestCreateRequest request = new FriendRequestCreateRequest();
        request.setReceiverUsername("bob");
        stubProfileByUsername("bob", OTHER_USER_ID);
        when(blockRepository.existsByBlockerIdAndBlockedId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(false);
        when(blockRepository.existsByBlockerIdAndBlockedId(OTHER_USER_ID, CURRENT_USER_ID))
                .thenReturn(true);

        var exception = assertThrows(AppException.class, () -> friendService.sendRequest(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BLOCKED);
    }

    @Test
    void sendRequest_blockedBySender_throwsBlocked() {
        FriendRequestCreateRequest request = new FriendRequestCreateRequest();
        request.setReceiverUsername("bob");
        stubProfileByUsername("bob", OTHER_USER_ID);
        when(blockRepository.existsByBlockerIdAndBlockedId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(true);

        var exception = assertThrows(AppException.class, () -> friendService.sendRequest(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BLOCKED);
    }

    @Test
    void sendRequest_toSelf_throwsCannotFriendYourself() {
        FriendRequestCreateRequest request = new FriendRequestCreateRequest();
        request.setReceiverUsername("self");
        stubProfileByUsername("self", CURRENT_USER_ID);
        when(blockRepository.existsByBlockerIdAndBlockedId(anyString(), anyString()))
                .thenReturn(false);

        var exception = assertThrows(AppException.class, () -> friendService.sendRequest(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FRIEND_YOURSELF);
    }

    @Test
    void sendRequest_alreadyPending_throwsAlreadyPending() {
        FriendRequestCreateRequest request = new FriendRequestCreateRequest();
        request.setReceiverUsername("bob");
        stubProfileByUsername("bob", OTHER_USER_ID);
        when(blockRepository.existsByBlockerIdAndBlockedId(anyString(), anyString()))
                .thenReturn(false);
        when(friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                        anyString(), anyString(), eq(FriendRequestStatus.ACCEPTED)))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findBySenderIdAndReceiverId(OTHER_USER_ID, CURRENT_USER_ID))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findBySenderIdAndReceiverId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(Optional.of(FriendRequest.builder()
                        .status(FriendRequestStatus.PENDING)
                        .build()));

        var exception = assertThrows(AppException.class, () -> friendService.sendRequest(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FRIEND_REQUEST_ALREADY_PENDING);
    }

    @Test
    void sendRequest_duplicateKeyRace_throwsAlreadyPending() {
        FriendRequestCreateRequest request = new FriendRequestCreateRequest();
        request.setReceiverUsername("bob");
        stubProfileByUsername("bob", OTHER_USER_ID);
        when(blockRepository.existsByBlockerIdAndBlockedId(anyString(), anyString()))
                .thenReturn(false);
        when(friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                        anyString(), anyString(), eq(FriendRequestStatus.ACCEPTED)))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findBySenderIdAndReceiverId(OTHER_USER_ID, CURRENT_USER_ID))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findBySenderIdAndReceiverId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.save(any())).thenThrow(new DuplicateKeyException("duplicate"));

        var exception = assertThrows(AppException.class, () -> friendService.sendRequest(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FRIEND_REQUEST_ALREADY_PENDING);
    }

    @Test
    void sendRequest_reverseRequestPending_autoAcceptsInsteadOfCreatingNew() {
        // B đã gửi request cho A trước đó (PENDING) -> A gửi lại cho B phải tự động ACCEPT,
        // không tạo request PENDING mới.
        FriendRequestCreateRequest request = new FriendRequestCreateRequest();
        request.setReceiverUsername("bob");
        stubProfileByUsername("bob", OTHER_USER_ID);
        when(blockRepository.existsByBlockerIdAndBlockedId(anyString(), anyString()))
                .thenReturn(false);
        when(friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                        anyString(), anyString(), eq(FriendRequestStatus.ACCEPTED)))
                .thenReturn(Optional.empty());

        FriendRequest reverseRequest = FriendRequest.builder()
                .id("req-reverse")
                .senderId(OTHER_USER_ID)
                .receiverId(CURRENT_USER_ID)
                .status(FriendRequestStatus.PENDING)
                .build();
        when(friendRequestRepository.findBySenderIdAndReceiverId(OTHER_USER_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(reverseRequest));
        when(friendRequestRepository.findBySenderIdAndReceiverId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());
        // sendRequest() enrich response bằng profile của sender (reverseRequest.senderId = OTHER_USER_ID)
        stubProfileExists(OTHER_USER_ID);
        when(friendMapper.toFriendRequestResponse(any(), any())).thenReturn(new FriendRequestResponse());

        friendService.sendRequest(request);

        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
        assertThat(captor.getValue().getId()).isEqualTo("req-reverse");
    }

    // ---- getFriendshipStatus() ----

    @Test
    void getFriendshipStatus_currentUserBlockedOther_returnsBlocked() {
        when(blockRepository.existsByBlockerIdAndBlockedId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(true);
        stubProfileExists(OTHER_USER_ID);
        when(friendMapper.toFriendResponse(eq(OTHER_USER_ID), eq("BLOCKED"), any()))
                .thenReturn(null);

        friendService.getFriendshipStatus(OTHER_USER_ID);

        verify(friendMapper).toFriendResponse(eq(OTHER_USER_ID), eq("BLOCKED"), any());
        // BLOCKED phải được trả về TRƯỚC khi kiểm tra bất kỳ status friendship nào khác.
        verify(friendRequestRepository, never()).findBySenderIdAndReceiverIdAndStatus(any(), any(), any());
    }

    @Test
    void getFriendshipStatus_otherBlockedCurrentUser_returnsBlockedBy() {
        when(blockRepository.existsByBlockerIdAndBlockedId(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(false);
        when(blockRepository.existsByBlockerIdAndBlockedId(OTHER_USER_ID, CURRENT_USER_ID))
                .thenReturn(true);
        stubProfileExists(OTHER_USER_ID);

        friendService.getFriendshipStatus(OTHER_USER_ID);

        verify(friendMapper).toFriendResponse(eq(OTHER_USER_ID), eq("BLOCKED_BY"), any());
    }

    // ---- helpers ----

    private void stubProfileExists(String userId) {
        UserProfileResponse profile =
                UserProfileResponse.builder().userId(userId).build();
        when(profileClient.getProfile(userId))
                .thenReturn(ApiResponse.<UserProfileResponse>builder()
                        .result(profile)
                        .build());
    }

    private void stubProfileByUsername(String username, String userId) {
        UserProfileResponse profile =
                UserProfileResponse.builder().userId(userId).build();
        when(profileClient.getProfileByUsername(username))
                .thenReturn(ApiResponse.<UserProfileResponse>builder()
                        .result(profile)
                        .build());
    }
}
