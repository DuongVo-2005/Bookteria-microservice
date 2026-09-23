package com.devteria.book.entity;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookPublisherInfo {
    @NotBlank(message = "BOOK_PUBLISHER_ID_REQUIRED")
    String publisherId;

    String name;
}
