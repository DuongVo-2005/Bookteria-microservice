package com.devteria.search.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec Phase 6 - "22.1 Search Integration" ("Group nổi bật"). Chỉ index group PUBLIC (xem
// GroupEventConsumer) - đây là index PHỤC VỤ TÌM KIẾM THEO TỪ KHOÁ (tên/mô tả), không phải nguồn
// xếp hạng "nổi bật" - xếp hạng theo memberCount/hoạt động đã có sẵn ở group-service's
// GET /groups/popular (Phase 6 §20), không duplicate lại ở đây.
@Document(indexName = "groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupDocument {
    @Id
    String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    String name;

    @Field(type = FieldType.Text)
    String description;

    @Field(type = FieldType.Keyword)
    String category;

    @Field(type = FieldType.Date)
    Instant createdAt;
}
