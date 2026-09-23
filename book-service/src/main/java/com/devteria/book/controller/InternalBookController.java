package com.devteria.book.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.BookResponse;
import com.devteria.book.service.BookService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec BA OPS-02: group-service's BookEventConsumer (Kafka consumer thread, không có
// HttpServletRequest/JWT của người dùng nào để forward) cần lấy lại snapshot sách gốc sau khi
// merge để re-point GroupPost.bookRef — bug thật bắt được lúc verify live: gọi thẳng
// GET /books/{bookId} (yêu cầu JWT) từ consumer thread làm AuthenticationRequestInterceptor NPE
// (RequestContextHolder.getRequestAttributes() == null ngoài luồng HTTP). Cùng pattern
// "/internal/**" permitAll đã dùng cho report-service's targetExists() (GAP-03).
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalBookController {
    BookService bookService;

    @GetMapping("/internal/books/{bookId}")
    ApiResponse<BookResponse> getBookById(@PathVariable String bookId) {
        return ApiResponse.<BookResponse>builder()
                .result(bookService.getBookById(bookId))
                .build();
    }
}
