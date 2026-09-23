package com.devteria.post.dto.response;

import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {
    String id;

    // idea-spec Phase 3 - "10.1 Rich Text & Mention": thiếu field này trước đây khiến không
    // resolve được username -> userId thật (chỉ có "id" nội bộ Neo4j của profile-service,
    // không phải userId dùng xuyên hệ thống) — khớp đúng field friend-service's
    // UserProfileResponse đã có.
    String userId;

    String firstName;
    String lastName;
    String username;
    LocalDate dob;
    String city;
}
