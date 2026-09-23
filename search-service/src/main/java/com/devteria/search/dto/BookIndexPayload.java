package com.devteria.search.dto;

import java.time.Instant;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Khớp JSON của BookResponse bên book-service (payload của BOOK_CREATED/BOOK_UPDATED trong
 * OutboxEventMessage). Tự định nghĩa riêng, không import class của book-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookIndexPayload {
    String id;
    String title;
    String subtitle;
    String description;
    String slug;
    String isbn13;
    String status;
    List<AuthorInfo> authors;
    List<CategoryInfo> categories;
    PublisherInfo publisher;
    MetadataInfo metadata;
    StatsInfo stats;
    Instant createdAt;
    Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AuthorInfo {
        String authorId;
        String name;
        String role;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CategoryInfo {
        String categoryId;
        String name;
        String slug;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PublisherInfo {
        String publisherId;
        String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MetadataInfo {
        String publishedDate;
        Integer pageCount;
        String language;
        String format;
        String edition;
        String coverImage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StatsInfo {
        java.math.BigDecimal ratingAverage;
        Long reviewCount;
        Long ratingCount;
        Long readCount;
        Long wantToReadCount;
    }
}
