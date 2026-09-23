package com.devteria.search.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Khớp JSON của group-service's GroupResponse (trả về từ GET /groups, dùng cho reindex).
 * Field name khác GroupIndexPayload (id thay vì groupId) vì đây là REST response thật của Group
 * entity - @JsonIgnoreProperties bắt buộc vì GroupResponse có nhiều field khác (memberCount,
 * joined, currentUserRole, ...) không cần cho index.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupListItemResponse {
    String id;
    String name;
    String description;
    String category;
    String visibility;
    Instant createdAt;
}
