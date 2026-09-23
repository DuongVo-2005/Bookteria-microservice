package com.devteria.post.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.devteria.post.configuration.AuthenticationRequestInterceptor;
import com.devteria.post.dto.ApiResponse;
import com.devteria.post.dto.response.BookLookupResponse;
import com.devteria.post.dto.response.PageResponse;
import com.devteria.post.dto.response.ReadingListItemResponse;

@FeignClient(
        name = "book-service",
        url = "${app.service.book.url}",
        configuration = AuthenticationRequestInterceptor.class)
public interface BookClient {
    @GetMapping("/books/{bookId}")
    ApiResponse<BookLookupResponse> getBookById(@PathVariable String bookId);

    // idea-spec Phase 6 - "22. Personalized Feed": lấy bookId trong shelf của caller (JWT
    // forward qua AuthenticationRequestInterceptor) để tìm post liên quan tới sách họ quan tâm.
    @GetMapping("/me/reading-list")
    ApiResponse<PageResponse<ReadingListItemResponse>> getMyReadingList(
            @RequestParam("page") int page, @RequestParam("size") int size);
}
