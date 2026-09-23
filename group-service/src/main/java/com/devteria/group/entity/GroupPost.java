package com.devteria.group.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.group.dto.GroupPostStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "group_posts")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupPost {
    @MongoId
    String id;

    @Indexed
    String groupId;

    String authorId;

    String content;

    BookRef bookRef;

    // idea-spec BA OPS-01. Field initializer chạy trong no-args constructor (Spring Data Mongo
    // dùng no-args ctor rồi set field theo key có trong document) nên post cũ trước khi có field
    // này (không có key "status" trong Mongo) vẫn deserialize đúng thành PUBLISHED, không phải
    // null — khác @Builder.Default boolean (vốn đã default false/0 kể cả không có initializer),
    // đây là field kiểu enum nên PHẢI có initializer tường minh mới an toàn ngược.
    @Builder.Default
    GroupPostStatus status = GroupPostStatus.PUBLISHED;

    String rejectionReason;

    @Indexed
    Instant createdAt;
}
