package com.devteria.chat.service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOServer;
import com.devteria.chat.dto.ConversationStatus;
import com.devteria.chat.dto.request.ConversationRequest;
import com.devteria.chat.dto.response.ConversationResponse;
import com.devteria.chat.dto.response.PageResponse;
import com.devteria.chat.dto.response.UserProfileResponse;
import com.devteria.chat.entity.Conversation;
import com.devteria.chat.entity.ParticipantInfo;
import com.devteria.chat.entity.WebSocketSession;
import com.devteria.chat.exception.AppException;
import com.devteria.chat.exception.ErrorCode;
import com.devteria.chat.mapper.ConversationMapper;
import com.devteria.chat.repository.ChatMessageRepository;
import com.devteria.chat.repository.ConversationRepository;
import com.devteria.chat.repository.WebSocketSessionRepository;
import com.devteria.chat.repository.httpclient.FriendClient;
import com.devteria.chat.repository.httpclient.ProfileClient;
import com.mongodb.DuplicateKeyException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {
    ConversationRepository conversationRepository;

    ChatMessageRepository chatMessageRepository;

    ProfileClient profileClient;

    FriendClient friendClient;

    ConversationMapper conversationMapper;

    SocketIOServer socketIOServer;

    WebSocketSessionRepository webSocketSessionRepository;

    public PageResponse<ConversationResponse> myConversations(int page, int size) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Page<Conversation> conversations = conversationRepository.findAllByParticipantIdsContainsAndNotHidden(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt")));

        return PageResponse.<ConversationResponse>builder()
                .currentPage(conversations.getNumber())
                .totalPages(conversations.getTotalPages())
                .pageSize(conversations.getSize())
                .totalElements(conversations.getTotalElements())
                .data(conversations.getContent().stream()
                        .map(this::toConversationResponse)
                        .toList())
                .build();
    }

    public void markAsRead(String conversationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Conversation conversation = getConversationOrThrow(conversationId);
        requireParticipant(conversation, userId);

        conversation.getLastReadAt().put(userId, Instant.now());
        conversationRepository.save(conversation);
    }

    public void hide(String conversationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Conversation conversation = getConversationOrThrow(conversationId);
        requireParticipant(conversation, userId);

        conversation.getHiddenBy().add(userId);
        conversationRepository.save(conversation);
    }

    public ConversationResponse create(ConversationRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        String participantId = request.getParticipantIds().getFirst();

        String friendshipStatus = getFriendshipStatus(participantId);
        if ("BLOCKED".equals(friendshipStatus) || "BLOCKED_BY".equals(friendshipStatus)) {
            throw new AppException(ErrorCode.BLOCKED);
        }

        // build hash
        String participantsHash = buildParticipantsHash(userId, participantId);

        Optional<Conversation> existingConversation = conversationRepository.findByParticipantsHash(participantsHash);
        if (existingConversation.isPresent()) {
            return toConversationResponse(existingConversation.get());
        }

        // get result
        var currentUserProfile = fetchProfileOrThrow(userId);
        var participantProfile = fetchProfileOrThrow(participantId);

        ParticipantInfo currentUser = toParticipantInfo(currentUserProfile);
        ParticipantInfo participant = toParticipantInfo(participantProfile);

        // Chưa là bạn (NONE/PENDING_SENT/PENDING_RECEIVED) -> conversation đi vào
        // trạng thái Message Request (PENDING), không phải chat bình thường ngay.
        boolean isFriend = "ACCEPTED".equals(friendshipStatus);
        ConversationStatus status = isFriend ? ConversationStatus.NORMAL : ConversationStatus.PENDING;
        String requestedBy = isFriend ? null : userId;

        Conversation conversation =
                buildConversation(request.getType(), participantsHash, currentUser, participant, status, requestedBy);
        conversation = saveOrGetExisting(conversation, participantsHash);

        return toConversationResponse(conversation);
    }

    // Danh sách Message Request đang chờ MÌNH phản hồi (không phải mình tự gửi).
    public List<ConversationResponse> getPendingRequests() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return conversationRepository.findPendingRequestsReceivedBy(userId).stream()
                .map(this::toConversationResponse)
                .toList();
    }

    public long getPendingRequestCount() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return conversationRepository.findPendingRequestsReceivedBy(userId).size();
    }

    public ConversationResponse acceptRequest(String conversationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Conversation conversation = getConversationOrThrow(conversationId);
        requireParticipant(conversation, userId);

        if (conversation.getStatus() != ConversationStatus.PENDING) {
            throw new AppException(ErrorCode.MESSAGE_REQUEST_NOT_PENDING);
        }
        // chỉ người NHẬN request (không phải người tạo) mới được Accept
        if (userId.equals(conversation.getRequestedBy())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        conversation.setStatus(ConversationStatus.NORMAL);
        conversation.setModifiedDate(Instant.now());
        conversation = conversationRepository.save(conversation);

        pushMessageRequestEvent("MESSAGE_REQUEST_ACCEPTED", conversation.getRequestedBy(), conversation);

        return toConversationResponse(conversation);
    }

    public ConversationResponse rejectRequest(String conversationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Conversation conversation = getConversationOrThrow(conversationId);
        requireParticipant(conversation, userId);

        if (conversation.getStatus() != ConversationStatus.PENDING) {
            throw new AppException(ErrorCode.MESSAGE_REQUEST_NOT_PENDING);
        }
        if (userId.equals(conversation.getRequestedBy())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        conversation.setStatus(ConversationStatus.REJECTED);
        conversation.setModifiedDate(Instant.now());
        conversation = conversationRepository.save(conversation);

        pushMessageRequestEvent("MESSAGE_REQUEST_REJECTED", conversation.getRequestedBy(), conversation);

        return toConversationResponse(conversation);
    }

    Conversation getConversationOrThrow(String conversationId) {
        return conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    void requireParticipant(Conversation conversation, String userId) {
        conversation.getParticipants().stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny()
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    void pushMessageRequestEvent(String eventType, String targetUserId, Conversation conversation) {
        if (targetUserId == null) {
            return;
        }
        Map<String, WebSocketSession> sessionsBySocketId =
                webSocketSessionRepository.findAllByUserIdIn(List.of(targetUserId)).stream()
                        .collect(Collectors.toMap(WebSocketSession::getSocketSessionId, Function.identity()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("conversationId", conversation.getId());

        socketIOServer.getAllClients().forEach(client -> {
            if (sessionsBySocketId.containsKey(client.getSessionId().toString())) {
                client.sendEvent("message-request-event", payload);
            }
        });
    }

    private Conversation saveOrGetExisting(Conversation conversation, String hash) {
        try {
            conversation = conversationRepository.save(conversation);
        } catch (DuplicateKeyException exception) {
            conversation = conversationRepository.findByParticipantsHash(hash).orElseThrow(() -> exception);
        }
        return conversation;
    }

    private Conversation buildConversation(
            String type,
            String hash,
            ParticipantInfo userInfo,
            ParticipantInfo participantInfo,
            ConversationStatus status,
            String requestedBy) {
        return Conversation.builder()
                .type(type)
                .participantsHash(hash)
                .createdDate(Instant.now())
                .modifiedDate(Instant.now())
                .participants(List.of(userInfo, participantInfo))
                .status(status)
                .requestedBy(requestedBy)
                .build();
    }

    private ParticipantInfo toParticipantInfo(UserProfileResponse profile) {
        return ParticipantInfo.builder()
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .avatar(profile.getAvatar())
                .build();
    }

    private String buildParticipantsHash(String userId, String participantId) {
        if (userId.equals(participantId)) {
            throw new AppException(ErrorCode.INVALID_VALUE);
        }
        return generateParticipantHash(
                List.of(userId, participantId).stream().sorted().toList());
    }

    private String getFriendshipStatus(String otherUserId) {
        var response = friendClient.getFriendshipStatus(otherUserId);
        if (response == null || response.getResult() == null) {
            return "NONE";
        }
        return response.getResult().getStatus();
    }

    private UserProfileResponse fetchProfileOrThrow(String id) {
        var ob = profileClient.getProfile(id);
        if (Objects.isNull(ob) || Objects.isNull(ob.getResult())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        return ob.getResult();
    }

    private String generateParticipantHash(List<String> ids) {
        StringJoiner stringJoiner = new StringJoiner("_");
        ids.forEach(stringJoiner::add);

        // SHA 256

        return stringJoiner.toString();
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        ConversationResponse conversationResponse = conversationMapper.toConversationResponse(conversation);

        conversation.getParticipants().stream()
                .filter(participantInfo -> !participantInfo.getUserId().equals(currentUserId))
                .findFirst()
                .ifPresent(participantInfo -> {
                    conversationResponse.setConversationName(participantInfo.getUsername());
                    conversationResponse.setConversationAvatar(participantInfo.getAvatar());
                });

        Instant lastRead = conversation.getLastReadAt().getOrDefault(currentUserId, Instant.EPOCH);
        conversationResponse.setUnreadCount(
                chatMessageRepository.countByConversationIdAndCreatedDateAfterAndSender_UserIdNot(
                        conversation.getId(), lastRead, currentUserId));

        return conversationResponse;
    }
}
