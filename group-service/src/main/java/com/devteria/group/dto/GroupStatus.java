package com.devteria.group.dto;

// idea-spec BA v2 §2.3: thay thế Group.hidden (boolean, chỉ dùng cho HIDE_GROUP report action) —
// SUSPENDED giữ đúng hành vi cũ của hidden=true (ẩn khỏi list/search/popular công khai, owner/
// member/ADMIN vẫn xem được); FROZEN là khái niệm MỚI (chỉ đọc - không cho đăng bài mới, vẫn hiện
// công khai bình thường); ACTIVE là mặc định.
public enum GroupStatus {
    ACTIVE,
    FROZEN,
    SUSPENDED
}
