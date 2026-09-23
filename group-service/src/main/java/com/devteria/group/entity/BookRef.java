package com.devteria.group.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookRef {
    String bookId;

    String bookTitle;

    String authorName;

    String coverImage;

    // idea-spec BA GAP-02: sách bị xoá ở book-service nhưng snapshot này chỉ nhúng, không phải FK
    // sống — trước đây vẫn giữ nguyên bookId cũ, FE click vào dễ crash/lỗi null. Consumer
    // "book-events" đánh dấu true khi nhận BOOK_DELETED, FE tự hiển thị "Sách không còn tồn tại".
    //
    // Tên field CỐ Ý là "deleted" chứ không phải "isDeleted" — bug thật bắt được lúc verify live:
    // Lombok @Builder sinh tên method builder theo field literal ("isDeleted(...)"), nhưng
    // MapStruct tra cứu property nguồn theo tên đã chuẩn hoá kiểu JavaBean từ getter
    // (isDeleted() -> property "deleted"), nên tìm builder method "deleted(...)" không khớp
    // "isDeleted(...)" -> MapStruct ÂM THẦM bỏ qua field này, không lỗi compile, không lỗi
    // runtime, giá trị luôn về false mặc định dù DB đã lưu đúng true (đã verify: Mongo document
    // có isDeleted:true thật nhưng API response luôn trả deleted:false). Đúng bài học
    // notification-service's Notification.read đã ghi trước đây, áp dụng ở chiều ngược lại.
    @Builder.Default
    boolean deleted = false;
}
