package com.devteria.post.dto;

import java.time.Instant;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec Phase 6 - "22.1 Search Integration": payload cho POST_CREATED, search-service
// consume để index. Chỉ index post PUBLIC - visibility đi kèm để consumer tự quyết định
// (không index sẵn ở nguồn, giữ logic filter tập trung 1 chỗ).
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostIndexEventPayload {
    String postId;
    String content;
    String authorId;
    List<String> hashtags;
    String bookId;
    String visibility;
    Instant createdDate;
}
