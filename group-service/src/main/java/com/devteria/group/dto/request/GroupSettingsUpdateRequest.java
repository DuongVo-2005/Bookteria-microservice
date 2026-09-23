package com.devteria.group.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA OPS-01: OWNER/ADMIN bật/tắt yêu cầu duyệt bài viết cho nhóm đã tồn tại.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupSettingsUpdateRequest {
    // Boolean (wrapper), không phải primitive - Spring Boot 4/Jackson 3 fail cứng
    // (MismatchedInputException) khi JSON thiếu/null map vào primitive boolean; đây lại là field
    // DUY NHẤT của request này nên trước đây thiếu field = luôn lỗi 400. Xem GroupService.updateGroupSettings.
    Boolean requireApproval;
}
