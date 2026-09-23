package com.devteria.chat.service;

import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.devteria.chat.entity.ProcessedKafkaEvent;
import com.devteria.chat.repository.ProcessedKafkaEventRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdempotencyService {
    ProcessedKafkaEventRepository processedKafkaEventRepository;

    // true nếu đây là lần đầu thấy eventId này (nên xử lý); false nếu đã xử lý rồi (nên bỏ qua).
    // Dùng unique _id làm chốt chặn thật ở tầng DB, existsById chỉ là fast-path tránh ghi thừa.
    public boolean markProcessedIfNew(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return true; // không có eventId (message cũ trước khi chuẩn hoá envelope) -> xử lý bình thường
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
