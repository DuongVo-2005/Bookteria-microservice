package com.devteria.search.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookSearchResponse {
    String id;

    String title;

    String subtitle;

    String slug;

    String isbn13;

    String status;

    List<String> authorNames;

    List<String> categoryNames;

    String publisherName;

    String coverImage;

    BigDecimal ratingAverage;

    Long ratingCount;

    Instant createdAt;
}
