package com.devteria.book.entity;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookCategoryInfo {
    @NotBlank(message = "BOOK_CATEGORY_ID_REQUIRED")
    String categoryId;

    String name;
    String slug;
}
