package com.devteria.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.notification.entity.DeviceToken;

public interface DeviceTokenRepository extends MongoRepository<DeviceToken, String> {
    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserId(String userId);

    void deleteByToken(String token);
}
