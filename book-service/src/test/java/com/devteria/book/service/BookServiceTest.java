package com.devteria.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import com.devteria.book.dto.BookViewability;
import com.devteria.book.dto.ReadingStatus;
import com.devteria.book.dto.response.BookResponse;
import com.devteria.book.dto.response.ReadingAccessResponse;
import com.devteria.book.entity.Book;
import com.devteria.book.entity.GoogleBooksAccessInfo;
import com.devteria.book.entity.ReadingList;
import com.devteria.book.entity.Review;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.mapper.BookMapper;
import com.devteria.book.repository.AuthorRepository;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.CategoryRepository;
import com.devteria.book.repository.PublisherRepository;
import com.devteria.book.repository.ReadingListRepository;
import com.devteria.book.repository.ReviewRepository;

// Unit test thuần Mockito cho BookService.getReadingAccess() — đúng bảng quyết định canRead
// ở docs/google-books-integration-tasks.md mục 7.
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private static final String BOOK_ID = "book-1";

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private Validator validator;

    @Mock
    private RedisTemplate<String, BookResponse> bookRedisTemplate;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private ReadingListRepository readingListRepository;

    @Mock
    private ReviewRepository reviewRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(
                bookRepository,
                authorRepository,
                categoryRepository,
                publisherRepository,
                bookMapper,
                validator,
                bookRedisTemplate,
                outboxEventService,
                readingListRepository,
                reviewRepository);
    }

    private Book bookWithAccess(GoogleBooksAccessInfo access) {
        return Book.builder()
                .id(BOOK_ID)
                .googleBookId("google-1")
                .googleBooksAccess(access)
                .build();
    }

    @Test
    void getReadingAccess_bookNotFound_throwsBookNotFound() {
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(AppException.class, () -> bookService.getReadingAccess(BOOK_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void getReadingAccess_noGoogleBooksAccess_returnsCanReadFalse() {
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookWithAccess(null)));

        ReadingAccessResponse response = bookService.getReadingAccess(BOOK_ID);

        assertThat(response.isCanRead()).isFalse();
    }

    @Test
    void getReadingAccess_allPagesEmbeddable_returnsCanReadTrue() {
        GoogleBooksAccessInfo access = GoogleBooksAccessInfo.builder()
                .viewability(BookViewability.ALL_PAGES)
                .embeddable(true)
                .build();
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookWithAccess(access)));

        ReadingAccessResponse response = bookService.getReadingAccess(BOOK_ID);

        assertThat(response.isCanRead()).isTrue();
    }

    @Test
    void getReadingAccess_partialEmbeddable_returnsCanReadTrue() {
        GoogleBooksAccessInfo access = GoogleBooksAccessInfo.builder()
                .viewability(BookViewability.PARTIAL)
                .embeddable(true)
                .build();
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookWithAccess(access)));

        ReadingAccessResponse response = bookService.getReadingAccess(BOOK_ID);

        assertThat(response.isCanRead()).isTrue();
    }

    @Test
    void getReadingAccess_allPagesNotEmbeddableWithWebReaderLink_returnsCanReadTrue() {
        GoogleBooksAccessInfo access = GoogleBooksAccessInfo.builder()
                .viewability(BookViewability.ALL_PAGES)
                .embeddable(false)
                .webReaderLink("https://books.google.com/reader")
                .build();
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookWithAccess(access)));

        ReadingAccessResponse response = bookService.getReadingAccess(BOOK_ID);

        assertThat(response.isCanRead()).isTrue();
    }

    @Test
    void getReadingAccess_allPagesNotEmbeddableNoWebReaderLink_returnsCanReadFalse() {
        GoogleBooksAccessInfo access = GoogleBooksAccessInfo.builder()
                .viewability(BookViewability.ALL_PAGES)
                .embeddable(false)
                .build();
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookWithAccess(access)));

        ReadingAccessResponse response = bookService.getReadingAccess(BOOK_ID);

        assertThat(response.isCanRead()).isFalse();
    }

    @Test
    void getReadingAccess_noPages_alwaysReturnsCanReadFalse() {
        GoogleBooksAccessInfo access = GoogleBooksAccessInfo.builder()
                .viewability(BookViewability.NO_PAGES)
                .embeddable(true)
                .webReaderLink("https://books.google.com/reader")
                .build();
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookWithAccess(access)));

        ReadingAccessResponse response = bookService.getReadingAccess(BOOK_ID);

        assertThat(response.isCanRead()).isFalse();
    }

    // ---- mergeBooks() (OPS-02) ----

    private static final String TARGET_BOOK_ID = "book-target";
    private static final String DUPLICATE_BOOK_ID = "book-duplicate";

    @Test
    void mergeBooks_sameId_throwsCannotMergeWithItself() {
        var exception = assertThrows(AppException.class, () -> bookService.mergeBooks(TARGET_BOOK_ID, TARGET_BOOK_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_MERGE_BOOK_WITH_ITSELF);
    }

    @Test
    void mergeBooks_targetNotFound_throwsBookNotFound() {
        when(bookRepository.findById(TARGET_BOOK_ID)).thenReturn(Optional.empty());

        var exception =
                assertThrows(AppException.class, () -> bookService.mergeBooks(TARGET_BOOK_ID, DUPLICATE_BOOK_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void mergeBooks_duplicateNotFound_throwsBookNotFound() {
        when(bookRepository.findById(TARGET_BOOK_ID))
                .thenReturn(Optional.of(Book.builder().id(TARGET_BOOK_ID).build()));
        when(bookRepository.existsById(DUPLICATE_BOOK_ID)).thenReturn(false);

        var exception =
                assertThrows(AppException.class, () -> bookService.mergeBooks(TARGET_BOOK_ID, DUPLICATE_BOOK_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void mergeBooks_movesReviewsAndReadingLists_thenDeletesDuplicate() {
        Book targetBook = Book.builder().id(TARGET_BOOK_ID).slug("target-slug").build();
        Book duplicateBook =
                Book.builder().id(DUPLICATE_BOOK_ID).slug("duplicate-slug").build();

        // user-a chỉ review sách trùng -> chuyển sang sách gốc.
        Review reviewOnlyOnDuplicate = Review.builder()
                .bookId(DUPLICATE_BOOK_ID)
                .userId("user-a")
                .rating(5)
                .build();
        // user-b đã review cả 2 -> bản trùng bị xoá, giữ bản trên sách gốc.
        Review reviewOnBoth = Review.builder()
                .bookId(DUPLICATE_BOOK_ID)
                .userId("user-b")
                .rating(3)
                .build();

        // user-c chỉ có sách trùng trên kệ -> chuyển sang sách gốc.
        ReadingList shelfOnlyOnDuplicate = ReadingList.builder()
                .bookId(DUPLICATE_BOOK_ID)
                .userId("user-c")
                .status(ReadingStatus.WANT_TO_READ)
                .build();
        // user-d đã có sách gốc trên kệ -> entry trùng bị xoá.
        ReadingList shelfOnBoth = ReadingList.builder()
                .bookId(DUPLICATE_BOOK_ID)
                .userId("user-d")
                .status(ReadingStatus.READING)
                .build();

        when(bookRepository.findById(TARGET_BOOK_ID)).thenReturn(Optional.of(targetBook));
        when(bookRepository.existsById(DUPLICATE_BOOK_ID)).thenReturn(true);
        when(bookRepository.findById(DUPLICATE_BOOK_ID)).thenReturn(Optional.of(duplicateBook));

        when(reviewRepository.findAllByBookId(DUPLICATE_BOOK_ID))
                .thenReturn(List.of(reviewOnlyOnDuplicate, reviewOnBoth));
        when(reviewRepository.existsByBookIdAndUserId(TARGET_BOOK_ID, "user-a")).thenReturn(false);
        when(reviewRepository.existsByBookIdAndUserId(TARGET_BOOK_ID, "user-b")).thenReturn(true);

        // 2 lần gọi thật trong luồng mergeBooks(): lần 1 từ mergeReadingLists() (còn 2 entry của
        // sách trùng), lần 2 từ deleteBook() gọi sau đó (2 entry đã move/xoá xong nên rỗng) -
        // thenReturn nối tiếp để khớp đúng thứ tự gọi thật, tránh stub sau ghi đè stub trước.
        when(readingListRepository.findAllByBookId(DUPLICATE_BOOK_ID))
                .thenReturn(List.of(shelfOnlyOnDuplicate, shelfOnBoth))
                .thenReturn(List.of());
        when(readingListRepository.existsByUserIdAndBookId("user-c", TARGET_BOOK_ID))
                .thenReturn(false);
        when(readingListRepository.existsByUserIdAndBookId("user-d", TARGET_BOOK_ID))
                .thenReturn(true);

        bookService.mergeBooks(TARGET_BOOK_ID, DUPLICATE_BOOK_ID);

        // Review: user-a chuyển sang sách gốc, user-b bị xoá bản trùng.
        assertThat(reviewOnlyOnDuplicate.getBookId()).isEqualTo(TARGET_BOOK_ID);
        verify(reviewRepository).delete(reviewOnBoth);
        verify(reviewRepository, never()).delete(reviewOnlyOnDuplicate);

        // ReadingList: user-c chuyển sang sách gốc, user-d bị xoá entry trùng.
        assertThat(shelfOnlyOnDuplicate.getBookId()).isEqualTo(TARGET_BOOK_ID);
        verify(readingListRepository).delete(shelfOnBoth);
        verify(readingListRepository, never()).delete(shelfOnlyOnDuplicate);

        // Sách trùng bị xoá thật + publish đúng 2 event (BOOK_MERGED cho re-point, BOOK_DELETED từ
        // deleteBook(), BOOK_UPDATED cho sách gốc sau merge).
        verify(bookRepository).delete(duplicateBook);
        verify(outboxEventService, times(3)).recordEvent(any(), any(), any());
    }

    // ---- setReviewsLocked() (BA v2 §2.1/P1-02) ----

    @Test
    void setReviewsLocked_bookNotFound_throws() {
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(AppException.class, () -> bookService.setReviewsLocked(BOOK_ID, true));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void setReviewsLocked_locksAndUnlocks() {
        Book book = Book.builder().id(BOOK_ID).reviewsLocked(false).build();
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookService.setReviewsLocked(BOOK_ID, true);

        assertThat(book.isReviewsLocked()).isTrue();
    }
}
