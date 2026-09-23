package com.devteria.post.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.post.dto.PostVisibility;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(value = "post")
public class Post {
    @MongoId
    String id;

    @Indexed
    String userId;

    String content;

    // null = sách tạo trước Phase 3 (Post Visibility) — coi như PUBLIC ở tầng service,
    // không migrate dữ liệu cũ.
    PostVisibility visibility;

    // idea-spec Phase 3 - "10. Book Attachment": null nếu post không gắn sách.
    String bookId;

    List<String> mentionedUserIds;

    List<String> hashtags;

    // idea-spec Phase 3 - "9. Post Media": chỉ lưu URL/fileName đã upload sẵn qua
    // file-service (client tự gọi POST /file/media/upload trước) — xem Known Issues
    // trong be-report.md về giới hạn thật của file-service hiện tại.
    List<String> mediaUrls;

    // idea-spec Phase 3 - "10.2 Share/Re-post": null nếu là post gốc.
    @Indexed
    String sharedFromPostId;

    @Builder.Default
    int likesCount = 0;

    @Builder.Default
    int commentsCount = 0;

    Instant createdDate;
    Instant modifiedDate;
}
