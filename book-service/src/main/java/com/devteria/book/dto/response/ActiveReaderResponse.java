package com.devteria.book.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-01 - Bước 3: gợi ý 5 "độc giả tích cực" để follow. Không có khái niệm follow
// (asymmetric) nào trong hệ thống hiện tại (friend-service chỉ có kết bạn 2 chiều) — phạm vi backend
// dừng ở gợi ý đúng 5 userId "tích cực nhất" (đo bằng số review đã viết), FE dùng chính
// POST /friend/friends/requests hiện có để thực hiện hành động "theo dõi" người dùng chọn, không
// xây riêng 1 hệ thống quan hệ follow mới cho 1 bước onboarding. Xem Known Issues trong be-report.md.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActiveReaderResponse {
    String userId;
    long reviewCount;
}
