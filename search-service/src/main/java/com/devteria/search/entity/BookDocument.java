package com.devteria.search.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Document(indexName = "books")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookDocument {
    @Id
    String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    String title;

    @Field(type = FieldType.Text)
    String subtitle;

    @Field(type = FieldType.Text)
    String description;

    @Field(type = FieldType.Keyword)
    String slug;

    @Field(type = FieldType.Keyword)
    String isbn13;

    @Field(type = FieldType.Keyword)
    String status;

    @Field(type = FieldType.Text)
    List<String> authorNames;

    @Field(type = FieldType.Keyword)
    List<String> authorIds;

    @Field(type = FieldType.Text)
    List<String> categoryNames;

    @Field(type = FieldType.Keyword)
    List<String> categoryIds;

    @Field(type = FieldType.Text)
    String publisherName;

    @Field(type = FieldType.Keyword)
    String publisherId;

    @Field(type = FieldType.Keyword, index = false)
    String coverImage;

    @Field(type = FieldType.Double)
    BigDecimal ratingAverage;

    @Field(type = FieldType.Long)
    Long ratingCount;

    @Field(type = FieldType.Date)
    Instant createdAt;
}
