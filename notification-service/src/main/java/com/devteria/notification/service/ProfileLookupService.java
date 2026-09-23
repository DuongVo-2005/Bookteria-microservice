package com.devteria.notification.service;

import org.springframework.stereotype.Service;

import com.devteria.notification.repository.httpclient.ProfileClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

// Dùng chung cho FriendEventConsumer/GroupEventConsumer — cả 2 đều cần resolve username từ
// userId để dựng câu thông báo có tên thật, không có sẵn trong mọi payload Kafka.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProfileLookupService {
    private static final String FALLBACK_NAME = "Một người dùng";

    ProfileClient profileClient;

    public String resolveUsername(String userId) {
        if (userId == null || userId.isBlank()) {
            return FALLBACK_NAME;
        }
        try {
            var response = profileClient.getProfile(userId);
            if (response != null
                    && response.getResult() != null
                    && response.getResult().getUsername() != null) {
                return response.getResult().getUsername();
            }
        } catch (Exception e) {
            log.warn("Could not resolve username for userId={}", userId, e);
        }
        return FALLBACK_NAME;
    }
}
