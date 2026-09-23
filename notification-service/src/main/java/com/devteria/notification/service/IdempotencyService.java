package com.devteria.notification.service;

import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.devteria.notification.entity.ProcessedKafkaEvent;
import com.devteria.notification.repository.ProcessedKafkaEventRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec Phase 2 - "3. Notification Idempotency". Copy đúng pattern book-service/chat-service.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdempotencyService {
    ProcessedKafkaEventRepository processedKafkaEventRepository;

    // true nếu đây là lần đầu thấy eventId này (nên xử lý); false nếu đã xử lý rồi (nên bỏ qua).
    public boolean markProcessedIfNew(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return true;
        }
        if (processedKafkaEventRepository.existsById(eventId)) {
            return false;
        }
        try {
            processedKafkaEventRepository.save(ProcessedKafkaEvent.builder()
                    .id(eventId)
                    .processedAt(Instant.now())
                    .build());
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
