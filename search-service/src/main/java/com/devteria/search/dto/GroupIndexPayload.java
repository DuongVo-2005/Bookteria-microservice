package com.devteria.search.dto;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Khớp JSON của group-service's GroupIndexEventPayload (payload của GROUP_CREATED trong
 * OutboxEventMessage). Tự định nghĩa riêng, không import class của group-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupIndexPayload {
    String groupId;
    String name;
    String description;
    String category;
    String visibility;
    Instant createdAt;
}
