package com.devteria.book.client.dto;

import java.util.List;

import lombok.Data;

@Data
public class GoogleBookVolumeInfo {

    private String title;

    private String subtitle;

    private List<String> authors;

    private String publisher;

    private String publishedDate;

    private String description;

    private List<IndustryIdentifier> industryIdentifiers;

    private Integer pageCount;

    private String printType;

    private List<String> categories;

    private Double averageRating;

    private Integer ratingsCount;

    private String maturityRating;

    private Boolean allowAnonLogging;

    private String contentVersion;

    private ImageLinks imageLinks;

    private String language;

    private String previewLink;

    private String infoLink;
}
