package com.devteria.identity.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devteria.identity.entity.OutboxEvent;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findAllByPublishedFalseOrderByCreatedAtAsc();

    void deleteAllByPublishedTrueAndPublishedAtBefore(Instant threshold);
}
