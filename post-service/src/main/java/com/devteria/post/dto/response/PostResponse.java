package com.devteria.post.dto.response;

import java.time.Instant;
import java.util.List;

import com.devteria.post.dto.PostVisibility;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostResponse {
    String id;
    String content;
    String userId;
    String username;
    String created;
    Instant createdDate;
    Instant modifiedDate;

    PostVisibility visibility;

    String bookId;
    // Enrich thêm (không có sẵn trên entity Post) — null nếu post không gắn sách hoặc gọi
    // book-service lỗi (không chặn hiển thị post vì lý do này).
    String bookTitle;
    String bookCoverImage;

    List<String> mentionedUserIds;
    List<String> hashtags;
    List<String> mediaUrls;

    String sharedFromPostId;

    int likesCount;
    int commentsCount;

    // Enrich theo viewer hiện tại (không có sẵn trên entity Post — 1 post có isLiked khác nhau
    // tuỳ ai đang xem).
    boolean liked;
}
