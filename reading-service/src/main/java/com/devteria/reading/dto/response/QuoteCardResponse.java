package com.devteria.reading.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-04: Quote Card Generator - dữ liệu để FE tự vẽ thiệp (chọn font/kích thước/
// màu nền/ảnh nền là hoàn toàn phía FE, canvas rendering không phải việc của BE). BE chỉ cung cấp
// đúng nội dung + metadata cần thiết để căn chỉnh "Tên sách + Tác giả" như đề bài yêu cầu.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuoteCardResponse {
    String text;
    String note;
    String bookTitle;
    String authorName;
    String bookCoverImage;
}
