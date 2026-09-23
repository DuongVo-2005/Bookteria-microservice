package com.devteria.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.devteria.notification.dto.DevicePlatform;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceTokenRequest {
    @NotBlank(message = "DEVICE_TOKEN_REQUIRED")
    String token;

    @NotNull(message = "DEVICE_PLATFORM_REQUIRED")
    DevicePlatform platform;
}
