package com.devteria.chat.service;

import org.springframework.stereotype.Service;

import com.devteria.chat.entity.WebSocketSession;
import com.devteria.chat.repository.WebSocketSessionRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketSessionService {
    WebSocketSessionRepository webSocketSessionRepository;

    public WebSocketSession create(WebSocketSession webSocketSecssion) {
        return webSocketSessionRepository.save(webSocketSecssion);
    }

    public void deleteSession(String sessionId) {
        webSocketSessionRepository.deleteBySocketSessionId(sessionId);
    }
}
