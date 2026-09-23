package com.devteria.notification.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.devteria.notification.dto.request.ApiResponse;
import com.devteria.notification.dto.request.DeviceTokenRequest;
import com.devteria.notification.dto.response.NotificationResponse;
import com.devteria.notification.dto.response.PageResponse;
import com.devteria.notification.service.DeviceTokenService;
import com.devteria.notification.service.NotificationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationRestController {
    NotificationService notificationService;
    DeviceTokenService deviceTokenService;

    @GetMapping
    ApiResponse<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<NotificationResponse> result = notificationService.getNotifications(currentUserId(), page, size);
        return ApiResponse.<PageResponse<NotificationResponse>>builder()
                .result(PageResponse.<NotificationResponse>builder()
                        .currentPage(result.getNumber())
                        .totalPages(result.getTotalPages())
                        .pageSize(result.getSize())
                        .totalElements(result.getTotalElements())
                        .data(result.getContent())
                        .build())
                .build();
    }

    @GetMapping("/unread-count")
    ApiResponse<Long> getUnreadCount() {
        return ApiResponse.<Long>builder()
                .result(notificationService.getUnreadCount(currentUserId()))
                .build();
    }

    @PostMapping("/{notificationId}/read")
    ApiResponse<Void> markAsRead(@PathVariable String notificationId) {
        notificationService.markAsRead(currentUserId(), notificationId);
        return ApiResponse.<Void>builder().message("Marked as read").build();
    }

    @PostMapping("/read-all")
    ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead(currentUserId());
        return ApiResponse.<Void>builder().message("All marked as read").build();
    }

    @PostMapping("/device-tokens")
    ApiResponse<Void> registerDeviceToken(@RequestBody @Valid DeviceTokenRequest request) {
        deviceTokenService.register(
                currentUserId(), request.getToken(), request.getPlatform().name());
        return ApiResponse.<Void>builder().message("Device token registered").build();
    }

    @DeleteMapping("/device-tokens/{token}")
    ApiResponse<Void> unregisterDeviceToken(@PathVariable String token) {
        deviceTokenService.unregister(currentUserId(), token);
        return ApiResponse.<Void>builder().message("Device token unregistered").build();
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
