package com.devteria.book.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.book.dto.BookStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "books")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Book {
    @MongoId
    String id;

    @TextIndexed(weight = 3)
    String title;

    @TextIndexed(weight = 2)
    String subtitle;

    String isbn13;

    @TextIndexed(weight = 1)
    String description;

    List<BookAuthorInfo> authors;

    List<BookCategoryInfo> categories;

    BookMetadata metadata;

    BookStats stats;

    String slug;

    BookStatus status;

    Instant createdAt;

    Instant updatedAt;

    BookPublisherInfo publisher;

    String googleBookId;

    GoogleBooksAccessInfo googleBooksAccess;

    // idea-spec BA v2 §2.1/P1-02: LIBRARIAN/ADMIN khoá viết Review cho sách này khi phát hiện
    // Review Bombing - review cũ vẫn hiển thị bình thường, chỉ chặn tạo review MỚI.
    @Builder.Default
    boolean reviewsLocked = false;
}
