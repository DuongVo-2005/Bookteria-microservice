package com.devteria.book.dto.response;

import java.time.Instant;
import java.util.List;

import com.devteria.book.dto.BookStatus;
import com.devteria.book.entity.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookResponse {

    String id;

    String title;

    String subtitle;

    String slug;

    String isbn13;

    String description;

    List<BookAuthorInfo> authors;

    List<BookCategoryInfo> categories;

    BookPublisherInfo publisher;

    BookMetadata metadata;

    BookStats stats;

    BookStatus status;

    Instant createdAt;

    Instant updatedAt;

    String googleBookId;

    GoogleBooksAccessInfo googleBooksAccess;

    boolean reviewsLocked;
}
