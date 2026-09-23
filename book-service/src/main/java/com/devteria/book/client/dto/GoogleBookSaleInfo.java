package com.devteria.book.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GoogleBookSaleInfo {

    private String country;

    private String saleability;

    @JsonProperty("isEbook")
    private Boolean isEbook;

    private ListPrice listPrice;

    private ListPrice retailPrice;

    private String buyLink;

    @Data
    public static class ListPrice {

        private Double amount;

        private String currencyCode;
    }
}
