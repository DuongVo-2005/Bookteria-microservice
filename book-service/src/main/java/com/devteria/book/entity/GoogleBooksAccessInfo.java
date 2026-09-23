package com.devteria.book.entity;

import com.devteria.book.dto.BookViewability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleBooksAccessInfo {

    private String country;

    private BookViewability viewability;

    private Boolean embeddable;

    private Boolean publicDomain;

    private String webReaderLink;

    private Boolean pdfAvailable;

    private Boolean epubAvailable;
}
