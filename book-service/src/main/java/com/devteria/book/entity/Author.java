package com.devteria.book.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "authors")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Author {
    String id;
    String name;
    String bio;
    String avatarUrl;
}
