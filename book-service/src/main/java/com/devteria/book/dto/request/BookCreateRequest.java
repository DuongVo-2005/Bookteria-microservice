package com.devteria.book.dto.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.devteria.book.dto.BookStatus;
import com.devteria.book.entity.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookCreateRequest {

    String id;

    @NotBlank(message = "BOOK_TITLE_REQUIRED")
    String title;

    String subtitle;

    @Pattern(regexp = "^\\d{13}$", message = "BOOK_ISBN13_INVALID")
    String isbn13;

    String description;

    @Size(min = 1, message = "BOOK_AUTHORS_REQUIRED")
    List<@Valid BookAuthorInfo> authors;

    @Size(min = 1, message = "BOOK_CATEGORY_REQUIRED")
    List<@Valid BookCategoryInfo> category;

    @Valid
    BookMetadata metadata;

    BookStats stats;

    String slug;

    BookStatus status;

    Instant createdAt;

    Instant updatedAt;

    @NotNull(message = "BOOK_PUBLISHER_REQUIRED")
    @Valid
    BookPublisherInfo publishers;

    String googleBookId;

    GoogleBooksAccessInfo googleBooksAccess;
}
