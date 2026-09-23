package com.devteria.notification.dto.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Bản sao đúng field của group-service's GroupEventPayload — role khai String (không phải
// enum GroupMemberRole) để tránh phải mang theo enum class riêng, Jackson chỉ cần đúng tên
// literal (VD "ADMIN") nên String vẫn map đúng.
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
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
