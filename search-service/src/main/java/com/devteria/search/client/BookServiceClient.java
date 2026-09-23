package com.devteria.search.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.devteria.search.configuration.AuthenticationRequestInterceptor;
import com.devteria.search.dto.BookIndexPayload;
import com.devteria.search.dto.response.ApiResponse;
import com.devteria.search.dto.response.PageResponse;

@FeignClient(
        name = "book-service",
        url = "${app.services.book.url}",
        configuration = AuthenticationRequestInterceptor.class)
public interface BookServiceClient {
    @GetMapping("/books")
    ApiResponse<PageResponse<BookIndexPayload>> getBooks(
            @RequestParam("page") int page, @RequestParam("size") int size);
}
