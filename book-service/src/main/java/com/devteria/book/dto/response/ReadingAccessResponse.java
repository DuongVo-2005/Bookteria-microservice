package com.devteria.book.dto.response;

import com.devteria.book.dto.BookViewability;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingAccessResponse {
    String bookId;
    String googleBookId;
    boolean canRead;
    BookViewability viewability;
    Boolean embeddable;
    Boolean publicDomain;
    String webReaderLink;
}
