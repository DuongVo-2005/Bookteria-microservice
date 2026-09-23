package com.devteria.identity.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.devteria.identity.entity.ProcessedKafkaEvent;
import com.devteria.identity.repository.ProcessedKafkaEventRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdempotencyService {
    ProcessedKafkaEventRepository processedKafkaEventRepository;

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
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    // Không có TTL index tự động (khác Mongo) — tự dọn record quá 7 ngày, cùng chu kỳ với
    // OutboxPublisher's cleanup job.
    @Scheduled(cron = "0 30 3 * * *")
    public void cleanupOldEvents() {
        processedKafkaEventRepository.deleteAllByProcessedAtBefore(Instant.now().minus(7, ChronoUnit.DAYS));
    }
}
