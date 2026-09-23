package com.devteria.reading.dto.response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-04: subset của book-service's BookResponse, cùng shape đã dùng ở
// group-service/post-service's BookLookupResponse cho GET /internal/books/{bookId}.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookLookupResponse {
    String id;
    String title;
    List<AuthorInfo> authors;
    Metadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AuthorInfo {
        String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Metadata {
        String coverImage;
    }
}
