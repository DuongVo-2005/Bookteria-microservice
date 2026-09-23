package com.devteria.group.dto;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupEventPayload {
    String groupId;
    String groupName;
    String userId;
    String actorId;
    GroupMemberRole role;
    String postId;
    String commentId;

    // Danh sách userId cần đẩy realtime (vd: toàn bộ member trừ người thực hiện hành động) —
    // group-service tự tính sẵn vì chat-service không có quyền truy cập GroupMemberRepository.
    List<String> targetUserIds;
}
