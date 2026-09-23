package com.devteria.group.dto.response;

import java.time.Instant;

import com.devteria.group.dto.GroupMemberRole;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupMemberResponse {
    String id;
    String userId;
    String groupId;
    String username;
    String avatar;
    GroupMemberRole role;
    Instant joinedAt;
}
