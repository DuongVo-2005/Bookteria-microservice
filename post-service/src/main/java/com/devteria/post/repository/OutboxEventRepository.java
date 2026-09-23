package com.devteria.post.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.post.entity.OutboxEvent;

@Repository
public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {
    List<OutboxEvent> findAllByPublishedFalseOrderByCreatedAtAsc();

    void deleteAllByPublishedTrueAndPublishedAtBefore(Instant threshold);
}
