package com.devteria.book.entity;

import jakarta.validation.constraints.Positive;

import com.devteria.book.dto.BookFormat;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookMetadata {
    String publishedDate;

    @Positive(message = "BOOK_PAGE_COUNT_INVALID")
    Integer pageCount;

    String language;
    BookFormat format;
    String edition;
    String coverImage;
}
