package com.devteria.chat.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendStatusResponse {
    String userId;
    String username;
    String avatar;
    String status;
}
