package com.devteria.book.client.googlebooks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.devteria.book.client.dto.GoogleBookVolume;
import com.devteria.book.client.dto.GoogleBooksVolumeResponse;

@Component
public class GoogleBooksClient {

    private final RestClient restClient;
    private final String apiKey;

    public GoogleBooksClient(RestClient googleBooksRestClient, @Value("${google.books.api-key:}") String apiKey) {
        this.restClient = googleBooksRestClient;
        this.apiKey = apiKey;
    }

    public GoogleBooksVolumeResponse searchVolumes(String query, int startIndex, int maxResults) {

        return restClient
                .get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/volumes")
                            .queryParam("q", query)
                            .queryParam("startIndex", startIndex)
                            .queryParam("maxResults", maxResults);

                    if (apiKey != null && !apiKey.isBlank()) {
                        builder.queryParam("key", apiKey);
                    }

                    return builder.build();
                })
                .retrieve()
                .body(GoogleBooksVolumeResponse.class);
    }

    public GoogleBookVolume getVolumeById(String volumeId) {
        return restClient
                .get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/volumes/{volumeId}");

                    if (apiKey != null && !apiKey.isBlank()) {
                        builder.queryParam("key", apiKey);
                    }

                    return builder.build(volumeId);
                })
                .retrieve()
                .body(GoogleBookVolume.class);
    }
}
