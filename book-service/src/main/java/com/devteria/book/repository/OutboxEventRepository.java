package com.devteria.book.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.book.entity.OutboxEvent;

@Repository
public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {
    List<OutboxEvent> findAllByPublishedFalseOrderByCreatedAtAsc();
}
