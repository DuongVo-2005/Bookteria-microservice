package com.devteria.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.devteria.identity.dto.LockDuration;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA OPS-03
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LockUserRequest {
    @NotBlank(message = "LOCK_REASON_REQUIRED")
    String reason;

    @NotNull(message = "LOCK_DURATION_REQUIRED")
    LockDuration duration;
}
