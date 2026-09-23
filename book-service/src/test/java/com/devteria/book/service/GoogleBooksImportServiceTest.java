package com.devteria.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devteria.book.client.dto.GoogleBookAccessInfo;
import com.devteria.book.client.dto.GoogleBookVolume;
import com.devteria.book.client.dto.GoogleBookVolumeInfo;
import com.devteria.book.client.dto.GoogleBooksVolumeResponse;
import com.devteria.book.client.googlebooks.GoogleBooksClient;
import com.devteria.book.dto.OutboxEventType;
import com.devteria.book.dto.request.BookCreateRequest;
import com.devteria.book.dto.response.BatchImportResponse;
import com.devteria.book.dto.response.BookResponse;
import com.devteria.book.entity.Author;
import com.devteria.book.entity.Book;
import com.devteria.book.entity.Category;
import com.devteria.book.entity.Publisher;
import com.devteria.book.repository.AuthorRepository;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.CategoryRepository;
import com.devteria.book.repository.PublisherRepository;

// Unit test thuần Mockito (không Spring context) cho GoogleBooksImportService — theo đúng
// pattern FriendServiceTest/GroupServiceTest đã có trong project.
@ExtendWith(MockitoExtension.class)
class GoogleBooksImportServiceTest {

    private static final String GOOGLE_BOOK_ID = "zyTCAlFPjgYC";

    @Mock
    private GoogleBooksClient googleBooksClient;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private BookService bookService;

    @Mock
    private OutboxEventService outboxEventService;

    private GoogleBooksImportService googleBooksImportService;

    @BeforeEach
    void setUp() {
        googleBooksImportService = new GoogleBooksImportService(
                googleBooksClient,
                bookRepository,
                authorRepository,
                categoryRepository,
                publisherRepository,
                bookService,
                outboxEventService);
    }

    private GoogleBookVolume newVolume(String id, String title, List<String> authors, List<String> categories) {
        GoogleBookVolumeInfo info = new GoogleBookVolumeInfo();
        info.setTitle(title);
        info.setAuthors(authors);
        info.setCategories(categories);
        info.setPublisher("Acme Press");

        GoogleBookVolume volume = new GoogleBookVolume();
        volume.setId(id);
        volume.setVolumeInfo(info);
        volume.setAccessInfo(new GoogleBookAccessInfo());
        return volume;
    }

    @Test
    void importByGoogleBookId_alreadyImported_returnsExistingBookWithoutCallingGoogle() {
        Book existing = Book.builder().id("book-1").googleBookId(GOOGLE_BOOK_ID).build();
        when(bookRepository.findByGoogleBookId(GOOGLE_BOOK_ID)).thenReturn(Optional.of(existing));
        when(bookService.getBookById("book-1"))
                .thenReturn(BookResponse.builder().id("book-1").build());

        BookResponse response = googleBooksImportService.importByGoogleBookId(GOOGLE_BOOK_ID);

        assertThat(response.getId()).isEqualTo("book-1");
        verify(googleBooksClient, never()).getVolumeById(anyString());
        verify(bookService, never()).createBook(any());
    }

    @Test
    void importFromVolume_newVolume_findOrCreatesAuthorCategoryPublisherThenCreatesBook() {
        GoogleBookVolume volume = newVolume(GOOGLE_BOOK_ID, "Java Programming", List.of("John Doe"), List.of("Tech"));
        when(bookRepository.findByGoogleBookId(GOOGLE_BOOK_ID)).thenReturn(Optional.empty());

        // Author "John Doe" chưa tồn tại -> phải tạo mới.
        when(authorRepository.findByNameIgnoreCase("John Doe")).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenAnswer(invocation -> {
            Author author = invocation.getArgument(0);
            author.setId("author-1");
            return author;
        });

        // Category "Tech" đã tồn tại -> tái sử dụng, không tạo mới.
        Category existingCategory =
                Category.builder().id("cat-1").name("Tech").slug("tech").build();
        when(categoryRepository.findByNameIgnoreCase("Tech")).thenReturn(Optional.of(existingCategory));

        when(publisherRepository.findByNameIgnoreCase("Acme Press")).thenReturn(Optional.empty());
        when(publisherRepository.save(any(Publisher.class))).thenAnswer(invocation -> {
            Publisher publisher = invocation.getArgument(0);
            publisher.setId("pub-1");
            return publisher;
        });

        when(bookService.createBook(any(BookCreateRequest.class)))
                .thenReturn(BookResponse.builder()
                        .id("book-new")
                        .googleBookId(GOOGLE_BOOK_ID)
                        .build());

        BookResponse response = googleBooksImportService.importFromVolume(volume);

        assertThat(response.getId()).isEqualTo("book-new");
        verify(authorRepository).save(any(Author.class));
        verify(categoryRepository, never()).save(any());
        verify(bookService).createBook(any(BookCreateRequest.class));
        verify(outboxEventService).recordEvent("book-new", OutboxEventType.BOOK_IMPORTED, response);
    }

    @Test
    void importFromVolume_alreadyImported_returnsExistingBookWithoutCreatingAgain() {
        GoogleBookVolume volume = newVolume(GOOGLE_BOOK_ID, "Java Programming", List.of("John Doe"), List.of("Tech"));
        Book existing = Book.builder().id("book-1").googleBookId(GOOGLE_BOOK_ID).build();
        when(bookRepository.findByGoogleBookId(GOOGLE_BOOK_ID)).thenReturn(Optional.of(existing));
        when(bookService.getBookById("book-1"))
                .thenReturn(BookResponse.builder().id("book-1").build());

        BookResponse response = googleBooksImportService.importFromVolume(volume);

        assertThat(response.getId()).isEqualTo("book-1");
        verify(bookService, never()).createBook(any());
        verify(authorRepository, never()).save(any());
    }

    @Test
    void batchImportFromGoogle_countsSuccessSkippedAndFailedCorrectly() {
        GoogleBookVolume newOne = newVolume("new-id", "New Book", List.of("Jane Doe"), List.of("Tech"));
        GoogleBookVolume alreadyImported = newVolume("existing-id", "Existing Book", List.of(), List.of());
        GoogleBookVolume invalid = new GoogleBookVolume(); // id == null -> invalid

        GoogleBooksVolumeResponse searchResponse = new GoogleBooksVolumeResponse();
        searchResponse.setTotalItems(3);
        searchResponse.setItems(List.of(newOne, alreadyImported, invalid));

        when(googleBooksClient.searchVolumes("java", 0, 20)).thenReturn(searchResponse);
        when(bookRepository.existsByGoogleBookId("new-id")).thenReturn(false);
        when(bookRepository.existsByGoogleBookId("existing-id")).thenReturn(true);
        when(bookRepository.findByGoogleBookId("new-id")).thenReturn(Optional.empty());

        when(authorRepository.findByNameIgnoreCase("Jane Doe")).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenAnswer(invocation -> {
            Author author = invocation.getArgument(0);
            author.setId("author-2");
            return author;
        });
        when(categoryRepository.findByNameIgnoreCase("Tech"))
                .thenReturn(
                        Optional.of(Category.builder().id("cat-1").name("Tech").build()));
        when(publisherRepository.findByNameIgnoreCase("Acme Press"))
                .thenReturn(Optional.of(
                        Publisher.builder().id("pub-1").name("Acme Press").build()));
        when(bookService.createBook(any(BookCreateRequest.class)))
                .thenReturn(BookResponse.builder()
                        .id("book-new")
                        .googleBookId("new-id")
                        .build());

        BatchImportResponse result = googleBooksImportService.batchImportFromGoogle("java", 20, 0);

        assertThat(result.getSuccess()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
        verify(bookService, times(1)).createBook(any(BookCreateRequest.class));
    }
}
