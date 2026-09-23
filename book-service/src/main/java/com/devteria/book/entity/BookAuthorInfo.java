package com.devteria.book.entity;

import jakarta.validation.constraints.NotBlank;

import com.devteria.book.dto.AuthorRole;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookAuthorInfo {
    @NotBlank(message = "BOOK_AUTHOR_ID_REQUIRED")
    String authorId;

    String name;

    AuthorRole role;
}
