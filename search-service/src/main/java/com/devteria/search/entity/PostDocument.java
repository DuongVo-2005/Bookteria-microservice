package com.devteria.search.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec Phase 6 - "22.1 Search Integration". Chỉ index post PUBLIC (xem PostEventConsumer) -
// index này không bao giờ chứa post FRIENDS/PRIVATE, tôn trọng đúng visibility gốc.
@Document(indexName = "posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostDocument {
    @Id
    String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    String content;

    @Field(type = FieldType.Keyword)
    String authorId;

    @Field(type = FieldType.Keyword)
    List<String> hashtags;

    @Field(type = FieldType.Keyword)
    String bookId;

    @Field(type = FieldType.Date)
    Instant createdDate;
}
