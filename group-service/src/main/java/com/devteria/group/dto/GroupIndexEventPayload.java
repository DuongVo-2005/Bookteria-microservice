package com.devteria.group.dto;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec Phase 6 - "22.1 Search Integration": payload cho GROUP_CREATED, search-service
// consume để index. Chỉ index group PUBLIC - visibility đi kèm để consumer tự quyết định.
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupIndexEventPayload {
    String groupId;
    String name;
    String description;
    String category;
    String visibility;
    Instant createdAt;
}
