package com.devteria.chat.dto;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupEventPayload {

    String groupId;

    String groupName;

    String userId;

    String actorId;

    String role;

    String postId;

    String commentId;

    List<String> targetUserIds;
}
