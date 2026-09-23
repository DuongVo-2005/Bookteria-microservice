package com.devteria.chat.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.devteria.chat.dto.FriendEventMessage;
import com.devteria.chat.entity.WebSocketSession;
import com.devteria.chat.repository.WebSocketSessionRepository;
import com.devteria.chat.service.IdempotencyService;

// idea-spec BA v2 §2.2 follow-up (Phase 2 report Known Issue #1): chat-service trước đây không
// có unit test nào - test riêng UserLockEventConsumer vì đây là chỗ nhận cả USER_LOCKED (Phase
// 4 §11.1) lẫn USER_DEACTIVATED (BA v2 §2.2, mới thêm) để ngắt WebSocket đang mở.
@ExtendWith(MockitoExtension.class)
class UserLockEventConsumerTest {

    private static final String USER_ID = "user-1";
    private static final String EVENT_ID = "event-1";
    private static final String PAYLOAD = "{\"userId\":\"" + USER_ID + "\"}";

    @Mock
    private WebSocketSessionRepository webSocketSessionRepository;

    @Mock
    private SocketIOServer socketIOServer;

    @Mock
    private IdempotencyService idempotencyService;

    private UserLockEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new UserLockEventConsumer(webSocketSessionRepository, socketIOServer, idempotencyService);
    }

    private FriendEventMessage message(String eventType) {
        FriendEventMessage message = new FriendEventMessage();
        message.setEventId(EVENT_ID);
        message.setEventType(eventType);
        message.setAggregateId(USER_ID);
        message.setPayload(PAYLOAD);
        return message;
    }

    @Test
    void consume_userLocked_disconnectsTheirOpenSocketSession() {
        when(idempotencyService.markProcessedIfNew(EVENT_ID)).thenReturn(true);
        UUID sessionId = UUID.randomUUID();
        when(webSocketSessionRepository.findAllByUserIdIn(List.of(USER_ID)))
                .thenReturn(List.of(WebSocketSession.builder()
                        .socketSessionId(sessionId.toString())
                        .userId(USER_ID)
                        .build()));
        SocketIOClient client = mock(SocketIOClient.class);
        when(client.getSessionId()).thenReturn(sessionId);
        when(socketIOServer.getAllClients()).thenReturn(List.of(client));

        consumer.consume(message("USER_LOCKED"));

        verify(client).disconnect();
    }

    @Test
    void consume_userDeactivated_alsoDisconnects() {
        // idea-spec BA v2 §2.2: eventType mới thêm, cùng payload shape với USER_LOCKED.
        when(idempotencyService.markProcessedIfNew(EVENT_ID)).thenReturn(true);
        UUID sessionId = UUID.randomUUID();
        when(webSocketSessionRepository.findAllByUserIdIn(List.of(USER_ID)))
                .thenReturn(List.of(WebSocketSession.builder()
                        .socketSessionId(sessionId.toString())
                        .userId(USER_ID)
                        .build()));
        SocketIOClient client = mock(SocketIOClient.class);
        when(client.getSessionId()).thenReturn(sessionId);
        when(socketIOServer.getAllClients()).thenReturn(List.of(client));

        consumer.consume(message("USER_DEACTIVATED"));

        verify(client).disconnect();
    }

    @Test
    void consume_duplicateEventId_isIgnored() {
        when(idempotencyService.markProcessedIfNew(EVENT_ID)).thenReturn(false);

        consumer.consume(message("USER_LOCKED"));

        verify(webSocketSessionRepository, never()).findAllByUserIdIn(any());
    }

    @Test
    void consume_unrelatedEventType_isIgnored() {
        when(idempotencyService.markProcessedIfNew(EVENT_ID)).thenReturn(true);

        consumer.consume(message("USER_UNLOCKED"));

        verify(webSocketSessionRepository, never()).findAllByUserIdIn(any());
    }

    @Test
    void consume_userHasNoOpenSession_neverTouchesSocketServer() {
        when(idempotencyService.markProcessedIfNew(EVENT_ID)).thenReturn(true);
        when(webSocketSessionRepository.findAllByUserIdIn(List.of(USER_ID))).thenReturn(List.of());

        consumer.consume(message("USER_LOCKED"));

        verify(socketIOServer, never()).getAllClients();
    }
}
