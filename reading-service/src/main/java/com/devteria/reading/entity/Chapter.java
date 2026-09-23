package com.devteria.reading.entity;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chapters")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Chapter {
    @MongoId
    String id;

    @Indexed
    String bookId;

    int chapterNumber;

    String title;

    String subtitle;

    String content;

    int readTimeMinutes;
}
