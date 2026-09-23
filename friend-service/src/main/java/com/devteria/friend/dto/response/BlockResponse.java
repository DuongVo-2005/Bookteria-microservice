package com.devteria.friend.dto.response;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockResponse {
    String userId;
    String username;
    String avatar;
    Instant createdAt;
}
