package com.devteria.book.client.dto;

import java.util.List;

import lombok.Data;

@Data
public class GoogleBooksVolumeResponse {

    private String kind;

    private int totalItems;

    private List<GoogleBookVolume> items;
}
