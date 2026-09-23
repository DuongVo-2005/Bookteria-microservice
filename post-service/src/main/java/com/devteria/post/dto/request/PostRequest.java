package com.devteria.post.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import com.devteria.post.dto.PostVisibility;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostRequest {
    @NotBlank(message = "POST_CONTENT_REQUIRED")
    String content;

    // null -> PostService tự set PUBLIC (giữ nguyên hành vi cũ trước Phase 3).
    PostVisibility visibility;

    // idea-spec Phase 3 - "10. Book Attachment", optional.
    String bookId;

    // idea-spec Phase 3 - "9. Post Media", optional — client tự upload qua
    // file-service (POST /file/media/upload) trước, chỉ gửi lại URL/fileName ở đây.
    List<String> mediaUrls;
}
