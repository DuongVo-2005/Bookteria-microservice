package com.devteria.book.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GoogleBookEpub {

    @JsonProperty("isAvailable")
    private Boolean isAvailable;

    private String downloadLink;

    private String acsTokenLink;
}
