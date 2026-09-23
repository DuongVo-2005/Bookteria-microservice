package com.devteria.reading.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.reading.configuration.AuthenticationRequestInterceptor;
import com.devteria.reading.dto.response.ApiResponse;
import com.devteria.reading.dto.response.BookLookupResponse;

// idea-spec BA FEAT-04: lấy bookTitle/authorName thật cho Quote Card (post-service's shareToPost
// enrich sau KHI tạo Post, nhưng Quote Card cần render ẢNH trước khi có Post nào, nên
// reading-service phải tự tra cứu). Dùng "/internal/books/{id}" (permitAll, cùng pattern
// group-service/post-service đã dùng) - không phải "/books/{id}" (yêu cầu JWT, không hợp cho
// service-to-service không forward token gốc trong mọi ngữ cảnh).
@FeignClient(
        name = "book-service-internal",
        url = "${app.service.book.url}",
        configuration = AuthenticationRequestInterceptor.class)
public interface BookClient {
    @GetMapping("/internal/books/{bookId}")
    ApiResponse<BookLookupResponse> getBookById(@PathVariable("bookId") String bookId);
}
