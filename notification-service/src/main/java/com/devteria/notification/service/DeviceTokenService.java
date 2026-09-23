package com.devteria.notification.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.devteria.notification.entity.DeviceToken;
import com.devteria.notification.repository.DeviceTokenRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec Phase 1 - "1.4 Device Token / Push Notification": chỉ lưu trữ, CHƯA gửi push thật
// (cần Firebase Admin SDK + credentials thật - ngoài phạm vi "chuẩn bị nền tảng").
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceTokenService {
    DeviceTokenRepository deviceTokenRepository;

    public void register(String userId, String token, String platform) {
        deviceTokenRepository
                .findByToken(token)
                .ifPresentOrElse(
                        existing -> {
                            existing.setUserId(userId);
                            existing.setPlatform(platform);
                            deviceTokenRepository.save(existing);
                        },
                        () -> deviceTokenRepository.save(DeviceToken.builder()
                                .userId(userId)
                                .token(token)
                                .platform(platform)
                                .createdAt(Instant.now())
                                .build()));
    }

    public void unregister(String userId, String token) {
        deviceTokenRepository.findByToken(token).ifPresent(existing -> {
            if (existing.getUserId().equals(userId)) {
                deviceTokenRepository.deleteByToken(token);
            }
        });
    }
}
