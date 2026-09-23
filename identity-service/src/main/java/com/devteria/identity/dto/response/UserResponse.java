package com.devteria.identity.dto.response;

import java.time.Instant;
import java.util.Set;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String username;
    //    String firstName;
    //    String lastName;
    //    LocalDate dob;
    boolean locked;

    // idea-spec BA OPS-03: null nếu chưa từng bị khoá hoặc đang khoá PERMANENT.
    Instant lockedUntil;
    String lockReason;

    // idea-spec BA v2 §2.2: Soft Delete & Anonymize.
    boolean deactivated;

    Set<RoleResponse> roles;

    // idea-spec BA v2 P1-01: permission gán trực tiếp cho user, ngoài permission suy ra từ Role.
    Set<PermissionResponse> permissions;
}
