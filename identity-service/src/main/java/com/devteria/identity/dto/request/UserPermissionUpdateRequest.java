package com.devteria.identity.dto.request;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA v2 P1-01: danh sách permission ADMIN gán trực tiếp cho 1 user, thay thế toàn bộ
// (không phải thêm/bớt từng cái) - cùng convention với UserUpdateRequest.roles.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserPermissionUpdateRequest {
    List<String> permissions;
}
