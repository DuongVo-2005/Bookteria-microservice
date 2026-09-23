package com.devteria.search.dto.response;

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
public class PostSearchResponse {
    String id;
    String content;
    String authorId;
    List<String> hashtags;
    String bookId;
    Instant createdDate;
}
