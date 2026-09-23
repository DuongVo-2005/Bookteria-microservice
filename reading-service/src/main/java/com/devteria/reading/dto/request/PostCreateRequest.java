package com.devteria.reading.dto.request;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Subset của post-service's PostRequest - chỉ những field reading-service thật sự cần khi share
// Highlight/Note thành Post (idea-spec Phase 5 - "18.1 Book Quotes/Highlights Sharing").
// visibility để null -> post-service tự mặc định PUBLIC (giữ đúng hành vi hiện có, không cần
// duplicate lại enum PostVisibility ở service này chỉ để phục vụ 1 request field).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostCreateRequest {
    String content;
    String bookId;

    // idea-spec BA FEAT-04: Quote Card đã render (FE tạo ảnh, upload qua file-service trước,
    // gửi lại URL ở đây) - optional, share Highlight vẫn hoạt động dạng text-only như cũ nếu bỏ trống.
    List<String> mediaUrls;
}
