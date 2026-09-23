package com.devteria.identity.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devteria.identity.entity.ProcessedKafkaEvent;

@Repository
public interface ProcessedKafkaEventRepository extends JpaRepository<ProcessedKafkaEvent, String> {
    void deleteAllByProcessedAtBefore(Instant threshold);
}
