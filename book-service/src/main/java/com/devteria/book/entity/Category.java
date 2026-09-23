package com.devteria.book.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "categories")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category {
    String id;
    String name;
    String slug;
}
