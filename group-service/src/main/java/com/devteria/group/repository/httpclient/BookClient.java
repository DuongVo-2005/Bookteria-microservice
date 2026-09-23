package com.devteria.group.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.group.configuration.AuthenticationRequestInterceptor;
import com.devteria.group.dto.response.ApiResponse;
import com.devteria.group.dto.response.BookLookupResponse;

@FeignClient(
        name = "book-service",
        url = "${app.services.book.url}",
        configuration = AuthenticationRequestInterceptor.class)
public interface BookClient {
    @GetMapping("/books/{bookId}")
    ApiResponse<BookLookupResponse> getBookById(@PathVariable String bookId);
}
