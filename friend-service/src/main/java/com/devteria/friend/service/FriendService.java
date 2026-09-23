package com.devteria.friend.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.friend.dto.FriendBlockedEventPayload;
import com.devteria.friend.dto.FriendRequestStatus;
import com.devteria.friend.dto.OutboxEventType;
import com.devteria.friend.dto.request.FriendRequestCreateRequest;
import com.devteria.friend.dto.response.BlockResponse;
import com.devteria.friend.dto.response.FriendRequestResponse;
import com.devteria.friend.dto.response.FriendResponse;
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

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FriendService {
    FriendRequestRepository friendRequestRepository;
    ProfileClient profileClient;
    FriendMapper friendMapper;
    OutboxEventService outboxEventService;
    BlockRepository blockRepository;
    BlockMapper blockMapper;

    public void blockUser(String blockedId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        if (currentUserId.equals(blockedId)) {
            throw new AppException(ErrorCode.CANNOT_BLOCK_YOURSELF);
        }
        checkUserExists(blockedId);
        // Không gửi request nếu đã block
        if (blockRepository.existsByBlockerIdAndBlockedId(currentUserId, blockedId)) {
            throw new AppException(ErrorCode.ALREADY_BLOCKED);
        }
        Block block = Block.builder()
                .blockerId(currentUserId)
                .blockedId(blockedId)
                .createdAt(Instant.now())
                .build();

        try {
            blockRepository.save(block);
        } catch (DuplicateKeyException e) {
            // 2 request block gần như đồng thời cùng lọt qua check existsBy() ở
            // trên — unique index (blockerId, blockedId) chặn ở DB, coi như đã
            // block rồi (đúng ý nghĩa nghiệp vụ của race condition này).
            throw new AppException(ErrorCode.ALREADY_BLOCKED);
        }

        // cancel request
        cancelFriendRelationship(currentUserId, blockedId);

        // phát real time
        FriendBlockedEventPayload payload = FriendBlockedEventPayload.builder()
                .blockerId(currentUserId)
                .blockedId(blockedId)
                .build();

        outboxEventService.recordEvent(currentUserId, OutboxEventType.FRIEND_BLOCKED, payload);
    }

    public void unblockUser(String blockedId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        if (!blockRepository.existsByBlockerIdAndBlockedId(currentUserId, blockedId)) {
            throw new AppException(ErrorCode.NOT_BLOCKED);
        }
        blockRepository.deleteByBlockerIdAndBlockedId(currentUserId, blockedId);

        FriendBlockedEventPayload payload = FriendBlockedEventPayload.builder()
                .blockerId(currentUserId)
                .blockedId(blockedId)
                .build();

        outboxEventService.recordEvent(currentUserId, OutboxEventType.FRIEND_UNBLOCKED, payload);
    }

    public FriendRequestResponse sendRequest(FriendRequestCreateRequest request) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        String receiverId = getProfileByUsername(request.getReceiverUsername()).getUserId();
        if (blockRepository.existsByBlockerIdAndBlockedId(currentUserId, receiverId)
                || blockRepository.existsByBlockerIdAndBlockedId(receiverId, currentUserId)) {
            throw new AppException(ErrorCode.BLOCKED);
        }
        if (currentUserId.equals(receiverId)) {
            throw new AppException(ErrorCode.CANNOT_FRIEND_YOURSELF);
        }
        if (isFriend(currentUserId, receiverId)) {
            throw new AppException(ErrorCode.ALREADY_FRIENDS);
        }

        // Kiểm tra request hiện tại
        FriendRequest reverseRequest = friendRequestRepository
                .findBySenderIdAndReceiverId(receiverId, currentUserId)
                .orElse(null);
        // check request hiện tại

        FriendRequest existingRequest = friendRequestRepository
                .findBySenderIdAndReceiverId(currentUserId, receiverId)
                .orElse(null);
        if (existingRequest != null && existingRequest.getStatus() == FriendRequestStatus.PENDING) {
            throw new AppException(ErrorCode.FRIEND_REQUEST_ALREADY_PENDING);
        }
        // check chiều ngược lại

        if (reverseRequest != null && reverseRequest.getStatus() == FriendRequestStatus.PENDING) {
            reverseRequest.setStatus(FriendRequestStatus.ACCEPTED);
            reverseRequest.setUpdatedAt(Instant.now());
            friendRequestRepository.save(reverseRequest);

            FriendRequestResponse reverseResponse = toEnrichedResponse(reverseRequest);
            outboxEventService.recordEvent(
                    reverseRequest.getId(), OutboxEventType.FRIEND_REQUEST_ACCEPTED, reverseResponse);
            return reverseResponse;
        }

        // sử dụng lại document nếu request đã rejected hoặc cancelled
        FriendRequest friendRequest;
        if (existingRequest != null) {
            friendRequest = existingRequest;
            friendRequest.setStatus(FriendRequestStatus.PENDING);
            friendRequest.setUpdatedAt(Instant.now());
        } else {
            friendRequest = FriendRequest.builder()
                    .senderId(currentUserId)
                    .receiverId(receiverId)
                    .status(FriendRequestStatus.PENDING)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }
        try {
            friendRequestRepository.save(friendRequest);
        } catch (DuplicateKeyException e) {
            // 2 request sendRequest gần như đồng thời cùng lọt qua check
            // existingRequest==null ở trên — unique index (senderId, receiverId)
            // chặn ở DB, người kia đã tạo request trước, coi như đã pending rồi.
            throw new AppException(ErrorCode.FRIEND_REQUEST_ALREADY_PENDING);
        }
        FriendRequestResponse response = toEnrichedResponse(friendRequest);
        outboxEventService.recordEvent(friendRequest.getId(), OutboxEventType.FRIEND_REQUEST_SENT, response);

        return response;
    }

    // Chấp nhận lời mời

    public FriendRequestResponse acceptRequest(String requestId) {
        String currentId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        FriendRequest request = getFriendRequest(requestId);

        // chỉ receiver mới đc chấp nhận
        if (!request.getReceiverId().equals(currentId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new AppException(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
        }
        request.setStatus(FriendRequestStatus.ACCEPTED);

        request.setUpdatedAt(Instant.now());
        friendRequestRepository.save(request);

        FriendRequestResponse response = toEnrichedResponse(request);
        outboxEventService.recordEvent(request.getId(), OutboxEventType.FRIEND_REQUEST_ACCEPTED, response);

        return response;
    }

    // từ chối lời mời

    public FriendRequestResponse rejectRequest(String requestId) {
        String currentId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        FriendRequest request = getFriendRequest(requestId);

        if (!request.getReceiverId().equals(currentId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new AppException(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
        }
        request.setStatus(FriendRequestStatus.REJECTED);
        request.setUpdatedAt(Instant.now());

        friendRequestRepository.save(request);

        FriendRequestResponse response = toEnrichedResponse(request);
        outboxEventService.recordEvent(request.getId(), OutboxEventType.FRIEND_REQUEST_REJECTED, response);

        return response;
    }

    // Hủy lời mời

    public void cancelRequest(String requestId) {
        String currentId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        FriendRequest request = getFriendRequest(requestId);

        // chỉ sender mới được huỷ request của chính mình
        if (!request.getSenderId().equals(currentId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new AppException(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
        }
        request.setStatus(FriendRequestStatus.CANCELLED);

        request.setUpdatedAt(Instant.now());

        friendRequestRepository.save(request);
    }

    // Xóa bạn

    public void removeFriend(String friendUserId) {
        String currentId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        FriendRequest friendShip = findAcceptedFriendship(currentId, friendUserId);
        if (friendShip == null) {
            throw new AppException(ErrorCode.NOT_FRIENDS);
        }
        friendShip.setStatus(FriendRequestStatus.CANCELLED);
        friendShip.setUpdatedAt(Instant.now());
        friendRequestRepository.save(friendShip);

        outboxEventService.recordEvent(
                friendShip.getId(), OutboxEventType.FRIEND_REMOVED, toEnrichedResponse(friendShip));
    }

    // Lấy danh sách bạn bè

    public List<FriendResponse> getFriend() {
        String currentId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        List<FriendRequest> friendShip =
                friendRequestRepository.findBySenderIdAndStatus(currentId, FriendRequestStatus.ACCEPTED);

        List<FriendRequest> receivedFriendships =
                friendRequestRepository.findByReceiverIdAndStatus(currentId, FriendRequestStatus.ACCEPTED);

        return Stream.concat(friendShip.stream(), receivedFriendships.stream())
                .map(friendship -> {
                    String friendUserId = getOrderUserId(friendship, currentId);

                    UserProfileResponse profile = getProfile(friendUserId);

                    return friendMapper.toFriendResponse(friendUserId, FriendRequestStatus.ACCEPTED.name(), profile);
                })
                .toList();
    }

    // idea-spec Phase 3 - "5. Global Feed" (feed theo bạn bè): endpoint internal cho
    // post-service lấy list userId bạn bè để lọc feed, không cần enrich username/avatar
    // (khác getFriend() ở trên - endpoint đó phục vụ FE, cần enrich; đây chỉ cần ID để filter).
    public List<String> getFriendUserIds(String userId) {
        List<FriendRequest> friendShip =
                friendRequestRepository.findBySenderIdAndStatus(userId, FriendRequestStatus.ACCEPTED);

        List<FriendRequest> receivedFriendships =
                friendRequestRepository.findByReceiverIdAndStatus(userId, FriendRequestStatus.ACCEPTED);

        return Stream.concat(friendShip.stream(), receivedFriendships.stream())
                .map(friendship -> getOrderUserId(friendship, userId))
                .toList();
    }

    // Lấy request nhận được
    public List<FriendRequestResponse> getReceiveRequest() {
        String currentId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        return friendRequestRepository.findByReceiverIdAndStatus(currentId, FriendRequestStatus.PENDING).stream()
                .map(this::toEnrichedResponse)
                .toList();
    }

    // Lấy request đã gửi

    public List<FriendRequestResponse> getSentRequests() {
        String currentId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        return friendRequestRepository.findBySenderIdAndStatus(currentId, FriendRequestStatus.PENDING).stream()
                .map(this::toEnrichedResponse)
                .toList();
    }

    // Kiểm tra trạng thái friendship

    public FriendResponse getFriendshipStatus(String otherUserId) {
        String currentId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        if (blockRepository.existsByBlockerIdAndBlockedId(currentId, otherUserId)) {
            UserProfileResponse profile = getProfile(otherUserId);
            return friendMapper.toFriendResponse(otherUserId, "BLOCKED", profile);
        }
        if (blockRepository.existsByBlockerIdAndBlockedId(otherUserId, currentId)) {
            UserProfileResponse profile = getProfile(otherUserId);
            return friendMapper.toFriendResponse(otherUserId, "BLOCKED_BY", profile);
        }

        if (currentId.equals(otherUserId)) {
            throw new AppException(ErrorCode.CANNOT_FRIEND_YOURSELF);
        }

        // nếu là bạn
        FriendRequest friendship = findAcceptedFriendship(currentId, otherUserId);
        if (friendship != null) {
            UserProfileResponse profile = getProfile(otherUserId);

            return friendMapper.toFriendResponse(otherUserId, FriendRequestStatus.ACCEPTED.name(), profile);
        }

        // current user đã gửi request
        FriendRequest sentRequest = friendRequestRepository
                .findBySenderIdAndReceiverIdAndStatus(currentId, otherUserId, FriendRequestStatus.PENDING)
                .orElse(null);
        if (sentRequest != null) {
            UserProfileResponse profile = getProfile(otherUserId);

            return friendMapper.toFriendResponse(otherUserId, "PENDING_SENT", profile);
        }

        // current user đã nhận request

        FriendRequest receivedRequest = friendRequestRepository
                .findBySenderIdAndReceiverIdAndStatus(otherUserId, currentId, FriendRequestStatus.PENDING)
                .orElse(null);
        if (receivedRequest != null) {
            UserProfileResponse profile = getProfile(otherUserId);

            return friendMapper.toFriendResponse(otherUserId, "PENDING_RECEIVED", profile);
        }

        UserProfileResponse profile = getProfile(otherUserId);

        return friendMapper.toFriendResponse(otherUserId, "NONE", profile);
    }

    public List<BlockResponse> getBlockedUsers() {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        List<Block> blocks = blockRepository.findByBlockerId(currentUserId);

        return blocks.stream()
                .map(block -> {
                    UserProfileResponse profile = getProfile(block.getBlockedId());

                    return blockMapper.toBlockResponse(block.getBlockedId(), block.getCreatedAt(), profile);
                })
                .toList();
    }

    private void cancelFriendRelationship(String userId1, String userId2) {

        // A-> B
        FriendRequest requestA = friendRequestRepository
                .findBySenderIdAndReceiverId(userId1, userId2)
                .orElse(null);
        if (requestA != null
                && (requestA.getStatus() == FriendRequestStatus.PENDING
                        || requestA.getStatus() == FriendRequestStatus.ACCEPTED)) {
            requestA.setStatus(FriendRequestStatus.CANCELLED);
            requestA.setUpdatedAt(Instant.now());

            friendRequestRepository.save(requestA);
        }
        // B -> A
        FriendRequest requestB = friendRequestRepository
                .findBySenderIdAndReceiverId(userId2, userId1)
                .orElse(null);
        if (requestB != null
                && (requestB.getStatus() == FriendRequestStatus.PENDING
                        || requestB.getStatus() == FriendRequestStatus.ACCEPTED)) {
            requestB.setStatus(FriendRequestStatus.CANCELLED);
            requestB.setUpdatedAt(Instant.now());

            friendRequestRepository.save(requestB);
        }
    }

    private void checkUserExists(String userId) {
        getProfile(userId);
    }

    private UserProfileResponse getProfileByUsername(String username) {
        // profile-service trả 404 thật (không phải body rỗng dạng 200) khi username không tồn
        // tại — Feign mặc định ném FeignException.NotFound cho status 404, trước đây không bắt
        // nên thoát ra thành lỗi 500-class chung chung (9999) thay vì USER_NOT_FOUND rõ ràng.
        try {
            var response = profileClient.getProfileByUsername(username);
            if (response == null || response.getResult() == null) {
                throw new AppException(ErrorCode.USER_NOT_FOUND);
            }
            return response.getResult();
        } catch (FeignException.NotFound e) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private String getOrderUserId(FriendRequest friendship, String currentUserId) {
        if (friendship.getSenderId().equals(currentUserId)) {
            return friendship.getReceiverId();
        }
        return friendship.getSenderId();
    }

    private FriendRequest getFriendRequest(String requestId) {
        return friendRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
    }

    private UserProfileResponse getProfile(String userId) {
        var response = profileClient.getProfile(userId);
        if (response == null || response.getResult() == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return response.getResult();
    }

    // Gắn thêm senderUsername/senderAvatar vào FriendRequestResponse, dùng chung
    // cho cả response trả về REST lẫn payload ghi vào Outbox (Kafka), để bên nhận
    // (FE, chat-service) không còn phải nhận senderId thô không kèm username.
    private FriendRequestResponse toEnrichedResponse(FriendRequest entity) {
        UserProfileResponse senderProfile = getProfile(entity.getSenderId());
        return friendMapper.toFriendRequestResponse(entity, senderProfile);
    }

    private FriendRequest findAcceptedFriendship(String userId1, String userId2) {
        FriendRequest first = friendRequestRepository
                .findBySenderIdAndReceiverIdAndStatus(userId1, userId2, FriendRequestStatus.ACCEPTED)
                .orElse(null);
        if (first != null) {
            return first;
        }
        return friendRequestRepository
                .findBySenderIdAndReceiverIdAndStatus(userId2, userId1, FriendRequestStatus.ACCEPTED)
                .orElse(null);
    }

    private boolean isFriend(String userId1, String userId2) {
        return findAcceptedFriendship(userId1, userId2) != null;
    }
}
