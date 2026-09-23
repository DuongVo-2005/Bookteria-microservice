package com.devteria.book.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.devteria.book.dto.request.BookBatchImportRequest;
import com.devteria.book.dto.request.BookCreateRequest;
import com.devteria.book.dto.request.BookMergeRequest;
import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.BatchImportResponse;
import com.devteria.book.dto.response.BookResponse;
import com.devteria.book.dto.response.PageResponse;
import com.devteria.book.dto.response.ReadingAccessResponse;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.service.BookService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookController {
    BookService bookService;

    @GetMapping("/books")
    public ApiResponse<PageResponse<BookResponse>> getBooks(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "authorId", required = false) String authorId) {

        var books = bookService.getBooks(page, size, categoryId, authorId);
        return ApiResponse.<PageResponse<BookResponse>>builder().result(books).build();
    }

    // idea-spec Phase 6 - "19. Trending Books"
    @GetMapping("/books/trending")
    public ApiResponse<PageResponse<BookResponse>> getTrendingBooks(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        var books = bookService.getTrendingBooks(page, size);
        return ApiResponse.<PageResponse<BookResponse>>builder().result(books).build();
    }

    @GetMapping("/books/search")
    public ApiResponse<PageResponse<BookResponse>> searchBooks(
            @RequestParam(value = "q") String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        if (q == null || q.isBlank()) {
            throw new AppException(ErrorCode.BOOK_SEARCH_KEYWORD_REQUIRED);
        }
        var books = bookService.searchBooks(q, page, size);
        return ApiResponse.<PageResponse<BookResponse>>builder().result(books).build();
    }

    @GetMapping("/books/{bookId}")
    public ApiResponse<BookResponse> getBookById(@PathVariable(value = "bookId") String bookId) {
        var book = bookService.getBookById(bookId);
        return ApiResponse.<BookResponse>builder().result(book).build();
    }

    @GetMapping("/books/slug/{slug}")
    public ApiResponse<BookResponse> getBookBySlug(@PathVariable(value = "slug") String slug) {
        var book = bookService.getBookBySlug(slug);
        return ApiResponse.<BookResponse>builder().result(book).build();
    }

    @GetMapping("/books/category/{categoryId}")
    public ApiResponse<List<BookResponse>> getBooksByCategoryId(@PathVariable(value = "categoryId") String categoryId) {
        var books = bookService.getBookByCategoryId(categoryId);
        return ApiResponse.<List<BookResponse>>builder().result(books).build();
    }

    @GetMapping("/books/author/{authorId}")
    public ApiResponse<List<BookResponse>> getBooksByAuthorId(@PathVariable(value = "authorId") String authorId) {
        var books = bookService.getBookByAuthorId(authorId);
        return ApiResponse.<List<BookResponse>>builder().result(books).build();
    }

    @GetMapping("/books/{bookId}/reading-access")
    public ApiResponse<ReadingAccessResponse> getReadingAccess(@PathVariable(value = "bookId") String bookId) {
        var access = bookService.getReadingAccess(bookId);
        return ApiResponse.<ReadingAccessResponse>builder().result(access).build();
    }

    // idea-spec BA v2 P1-02: LIBRARIAN có quyền catalog qua permission JWT scope (book:create/
    // update/delete/import), ADMIN vẫn luôn qua được nhờ "Ultimate Authority" (§2.2) - OR thay vì
    // thay hẳn hasRole('ADMIN'), giữ ADMIN không phụ thuộc có gán permission book:* hay không.
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('book:create')")
    @PostMapping("/books")
    public ApiResponse<BookResponse> createBook(@RequestBody @Valid BookCreateRequest request) {
        var book = bookService.createBook(request);
        return ApiResponse.<BookResponse>builder().result(book).build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('book:update')")
    @PutMapping("/books/{bookId}")
    public ApiResponse<BookResponse> updateBook(
            @PathVariable(value = "bookId") String bookId, @RequestBody @Valid BookCreateRequest request) {
        var book = bookService.updateBook(bookId, request);
        return ApiResponse.<BookResponse>builder().result(book).build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('book:delete')")
    @DeleteMapping("/books/{bookId}")
    public ApiResponse<Void> deleteBook(@PathVariable(value = "bookId") String bookId) {
        bookService.deleteBook(bookId);
        return ApiResponse.<Void>builder().message("Book has been deleted").build();
    }

    // idea-spec BA OPS-02: Merge Books Tool — gộp bookId (sách trùng) vào :bookId (sách gốc).
    // idea-spec BA v2 P2-03: actor giờ là "LIBRARIAN, ADMIN" — book:create đủ vì cùng bản chất
    // thao tác catalog, không cần permission riêng cho merge.
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('book:create')")
    @PostMapping("/books/{bookId}/merge")
    public ApiResponse<BookResponse> mergeBooks(
            @PathVariable(value = "bookId") String bookId, @RequestBody @Valid BookMergeRequest request) {
        var book = bookService.mergeBooks(bookId, request.getDuplicateBookId());
        return ApiResponse.<BookResponse>builder().result(book).build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('book:import')")
    @PostMapping("/books/batch-import")
    public ApiResponse<BatchImportResponse> batchImportBook(@RequestBody BookBatchImportRequest request) {
        return ApiResponse.<BatchImportResponse>builder()
                .result(bookService.batchImportBooks(request))
                .build();
    }

    // idea-spec BA v2 §2.1/P1-02: "review:lock — Khoá chức năng viết Review cho một cuốn sách
    // nếu phát hiện tình trạng Review Bombing".
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('review:lock')")
    @PatchMapping("/books/{bookId}/reviews-lock")
    public ApiResponse<BookResponse> setReviewsLocked(
            @PathVariable(value = "bookId") String bookId, @RequestParam(value = "locked") boolean locked) {
        var book = bookService.setReviewsLocked(bookId, locked);
        return ApiResponse.<BookResponse>builder().result(book).build();
    }
}
