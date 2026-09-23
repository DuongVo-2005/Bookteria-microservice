package com.devteria.group.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.group.entity.OutboxEvent;

public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {
    List<OutboxEvent> findAllByPublishedFalseOrderByCreatedAtAsc();

    void deleteAllByPublishedTrueAndPublishedAtBefore(Instant threshold);
}
