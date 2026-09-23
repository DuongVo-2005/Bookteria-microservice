package com.devteria.book.dto.response;

import java.util.List;

import com.devteria.book.dto.BookViewability;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleBookSearchItemResponse {
    String googleBookId;
    String title;
    List<String> authors;
    String thumbnail;
    String description;
    String publishedDate;
    BookViewability viewability;
    Boolean embeddable;
    Boolean publicDomain;
    String webReaderLink;
}
