package com.devteria.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.notification.entity.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(String id, String userId);

    List<Notification> findByUserIdAndReadFalse(String userId);

    long countByUserIdAndReadFalse(String userId);

    Optional<Notification> findFirstByUserIdAndAggregationKeyAndReadFalseOrderByCreatedAtDesc(
            String userId, String aggregationKey);
}
