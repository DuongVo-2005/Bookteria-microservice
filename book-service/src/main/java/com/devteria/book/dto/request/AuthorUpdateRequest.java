package com.devteria.book.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA v2 P1-02: author:manage
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthorUpdateRequest {
    @NotBlank(message = "AUTHOR_NAME_REQUIRED")
    String name;

    String bio;
    String avatarUrl;
}
