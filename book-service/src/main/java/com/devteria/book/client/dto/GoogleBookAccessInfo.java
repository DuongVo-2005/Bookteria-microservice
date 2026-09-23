package com.devteria.book.client.dto;

import lombok.Data;

@Data
public class GoogleBookAccessInfo {

    private String country;

    private String viewability;

    private Boolean embeddable;

    private Boolean publicDomain;

    private String textToSpeechPermission;

    private GoogleBookEpub epub;

    private GoogleBookPdf pdf;

    private String webReaderLink;

    private String accessViewStatus;
}
