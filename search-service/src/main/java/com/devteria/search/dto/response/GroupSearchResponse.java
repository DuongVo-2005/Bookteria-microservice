package com.devteria.search.dto.response;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupSearchResponse {
    String id;
    String name;
    String description;
    String category;
    Instant createdAt;
}
