package com.devteria.reading.service;

import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.devteria.reading.entity.ProcessedKafkaEvent;
import com.devteria.reading.repository.ProcessedKafkaEventRepository;

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
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
