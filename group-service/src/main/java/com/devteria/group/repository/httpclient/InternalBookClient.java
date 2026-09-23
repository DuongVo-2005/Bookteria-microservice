package com.devteria.group.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.group.dto.response.ApiResponse;
import com.devteria.group.dto.response.BookLookupResponse;

// idea-spec BA OPS-02: dùng từ BookEventConsumer (Kafka consumer thread) — KHÔNG cấu hình
// AuthenticationRequestInterceptor như BookClient, vì thread này không có HttpServletRequest nào
// để forward JWT (đã bắt NPE thật lúc verify live). Gọi endpoint permitAll "/internal/books/{id}"
// thay vì "/books/{id}" (yêu cầu JWT).
@FeignClient(name = "book-service-internal", url = "${app.services.book.url}")
public interface InternalBookClient {
    @GetMapping("/internal/books/{bookId}")
    ApiResponse<BookLookupResponse> getBookById(@PathVariable String bookId);
}
