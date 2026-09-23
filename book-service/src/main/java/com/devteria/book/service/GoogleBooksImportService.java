package com.devteria.book.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.devteria.book.client.dto.GoogleBookAccessInfo;
import com.devteria.book.client.dto.GoogleBookVolume;
import com.devteria.book.client.dto.GoogleBookVolumeInfo;
import com.devteria.book.client.dto.GoogleBooksVolumeResponse;
import com.devteria.book.client.dto.IndustryIdentifier;
import com.devteria.book.client.googlebooks.GoogleBooksClient;
import com.devteria.book.dto.AuthorRole;
import com.devteria.book.dto.BookFormat;
import com.devteria.book.dto.BookStatus;
import com.devteria.book.dto.BookViewability;
import com.devteria.book.dto.OutboxEventType;
import com.devteria.book.dto.request.BookCreateRequest;
import com.devteria.book.dto.response.BatchImportResponse;
import com.devteria.book.dto.response.BookResponse;
import com.devteria.book.dto.response.GoogleBookSearchItemResponse;
import com.devteria.book.dto.response.PageResponse;
import com.devteria.book.entity.Author;
import com.devteria.book.entity.BookAuthorInfo;
import com.devteria.book.entity.BookCategoryInfo;
import com.devteria.book.entity.BookMetadata;
import com.devteria.book.entity.BookPublisherInfo;
import com.devteria.book.entity.Category;
import com.devteria.book.entity.GoogleBooksAccessInfo;
import com.devteria.book.entity.Publisher;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.exception.error.BatchImportError;
import com.devteria.book.repository.AuthorRepository;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.CategoryRepository;
import com.devteria.book.repository.PublisherRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GoogleBooksImportService {
    private static final String UNKNOWN_PUBLISHER = "Unknown";
    private static final String UNKNOWN_AUTHOR = "Unknown Author";
    private static final String UNKNOWN_CATEGORY = "Unknown";

    GoogleBooksClient googleBooksClient;
    BookRepository bookRepository;
    AuthorRepository authorRepository;
    CategoryRepository categoryRepository;
    PublisherRepository publisherRepository;
    BookService bookService;
    OutboxEventService outboxEventService;

    @Transactional
    public BookResponse importByGoogleBookId(String googleBookId) {
        return bookRepository
                .findByGoogleBookId(googleBookId)
                .map(book -> bookService.getBookById(book.getId()))
                .orElseGet(() -> importFromVolume(fetchVolume(googleBookId)));
    }

    private GoogleBookVolume fetchVolume(String googleBookId) {
        try {
            GoogleBookVolume volume = googleBooksClient.getVolumeById(googleBookId);
            if (volume == null || volume.getId() == null) {
                throw new AppException(ErrorCode.GOOGLE_BOOK_NOT_FOUND);
            }
            return volume;
        } catch (HttpClientErrorException.NotFound e) {
            throw new AppException(ErrorCode.GOOGLE_BOOK_NOT_FOUND);
        } catch (RestClientException e) {
            log.error("Google Books API call failed for googleBookId={}", googleBookId, e);
            throw new AppException(ErrorCode.GOOGLE_BOOKS_API_ERROR);
        }
    }

    @Transactional
    public BookResponse importFromVolume(GoogleBookVolume volume) {
        if (volume == null || volume.getId() == null) {
            throw new AppException(ErrorCode.GOOGLE_BOOK_NOT_FOUND);
        }

        var existingBook = bookRepository.findByGoogleBookId(volume.getId());
        if (existingBook.isPresent()) {
            return bookService.getBookById(existingBook.get().getId());
        }

        GoogleBookVolumeInfo volumeInfo = volume.getVolumeInfo();
        if (volumeInfo == null) {
            throw new AppException(ErrorCode.GOOGLE_BOOKS_API_ERROR);
        }

        List<BookAuthorInfo> authors = resolveAuthors(volumeInfo.getAuthors());
        List<BookCategoryInfo> categories = resolveCategories(volumeInfo.getCategories());
        BookPublisherInfo publisher = resolvePublisher(volumeInfo.getPublisher());
        String isbn13 = extractIsbn13(volumeInfo.getIndustryIdentifiers());

        BookMetadata metadata = BookMetadata.builder()
                .publishedDate(volumeInfo.getPublishedDate())
                .pageCount(
                        volumeInfo.getPageCount() != null && volumeInfo.getPageCount() > 0
                                ? volumeInfo.getPageCount()
                                : null)
                .language(volumeInfo.getLanguage())
                .format(BookFormat.EBOOK)
                .coverImage(
                        volumeInfo.getImageLinks() != null
                                ? volumeInfo.getImageLinks().getThumbnail()
                                : null)
                .build();

        BookCreateRequest request = BookCreateRequest.builder()
                .title(volumeInfo.getTitle())
                .subtitle(volumeInfo.getSubtitle())
                .isbn13(isbn13)
                .description(volumeInfo.getDescription())
                .authors(authors)
                .category(categories)
                .metadata(metadata)
                .status(BookStatus.PUBLISHED)
                .publishers(publisher)
                .googleBookId(volume.getId())
                .googleBooksAccess(mapAccessInfo(volume.getAccessInfo()))
                .build();

        BookResponse response = bookService.createBook(request);
        outboxEventService.recordEvent(response.getId(), OutboxEventType.BOOK_IMPORTED, response);
        return response;
    }

    public PageResponse<GoogleBookSearchItemResponse> searchGoogleBooks(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            throw new AppException(ErrorCode.BOOK_SEARCH_KEYWORD_REQUIRED);
        }
        int clampedSize = Math.min(Math.max(size, 1), 40);
        int startIndex = page * clampedSize;

        GoogleBooksVolumeResponse searchResponse;
        try {
            searchResponse = googleBooksClient.searchVolumes(query, startIndex, clampedSize);
        } catch (RestClientException e) {
            log.error("Google Books search failed for query={}", query, e);
            throw new AppException(ErrorCode.GOOGLE_BOOKS_API_ERROR);
        }

        List<GoogleBookSearchItemResponse> items = searchResponse != null && searchResponse.getItems() != null
                ? searchResponse.getItems().stream().map(this::toSearchItem).toList()
                : List.of();
        long totalItems = searchResponse != null ? searchResponse.getTotalItems() : 0;
        int totalPages = (int) Math.ceil((double) totalItems / clampedSize);

        return PageResponse.<GoogleBookSearchItemResponse>builder()
                .data(items)
                .currentPage(page)
                .pageSize(clampedSize)
                .totalElements(totalItems)
                .totalPages(totalPages)
                .build();
    }

    private GoogleBookSearchItemResponse toSearchItem(GoogleBookVolume volume) {
        GoogleBookVolumeInfo info = volume != null ? volume.getVolumeInfo() : null;
        GoogleBookAccessInfo access = volume != null ? volume.getAccessInfo() : null;

        return GoogleBookSearchItemResponse.builder()
                .googleBookId(volume != null ? volume.getId() : null)
                .title(info != null ? info.getTitle() : null)
                .authors(info != null ? info.getAuthors() : null)
                .thumbnail(
                        info != null && info.getImageLinks() != null
                                ? info.getImageLinks().getThumbnail()
                                : null)
                .description(info != null ? info.getDescription() : null)
                .publishedDate(info != null ? info.getPublishedDate() : null)
                .viewability(parseViewability(access != null ? access.getViewability() : null))
                .embeddable(access != null ? access.getEmbeddable() : null)
                .publicDomain(access != null ? access.getPublicDomain() : null)
                .webReaderLink(access != null ? access.getWebReaderLink() : null)
                .build();
    }

    @Transactional
    public BatchImportResponse batchImportFromGoogle(String query, int maxResults, int startIndex) {
        int clampedMaxResults = Math.min(Math.max(maxResults, 1), 40);

        GoogleBooksVolumeResponse searchResponse;
        try {
            searchResponse = googleBooksClient.searchVolumes(query, startIndex, clampedMaxResults);
        } catch (RestClientException e) {
            log.error("Google Books search failed for query={}", query, e);
            throw new AppException(ErrorCode.GOOGLE_BOOKS_API_ERROR);
        }

        List<GoogleBookVolume> items =
                searchResponse != null && searchResponse.getItems() != null ? searchResponse.getItems() : List.of();

        int success = 0;
        int failed = 0;
        int skipped = 0;
        List<BatchImportError> errors = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            GoogleBookVolume item = items.get(i);
            if (item == null || item.getId() == null) {
                failed++;
                errors.add(BatchImportError.builder()
                        .index(i)
                        .message("Invalid Google volume")
                        .build());
                continue;
            }
            if (bookRepository.existsByGoogleBookId(item.getId())) {
                skipped++;
                continue;
            }
            try {
                importFromVolume(item);
                success++;
            } catch (AppException e) {
                failed++;
                errors.add(BatchImportError.builder()
                        .index(i)
                        .message(e.getErrorCode().getMessage())
                        .build());
            } catch (Exception e) {
                failed++;
                errors.add(BatchImportError.builder()
                        .index(i)
                        .message("Unexpected error: " + e.getClass().getSimpleName())
                        .build());
                log.warn("Batch import from Google failed at index {}", i, e);
            }
        }

        return BatchImportResponse.builder()
                .success(success)
                .failed(failed)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    private List<BookAuthorInfo> resolveAuthors(List<String> authorNames) {
        List<BookAuthorInfo> authors = new ArrayList<>();
        if (authorNames != null) {
            for (String authorName : authorNames) {
                if (authorName == null || authorName.isBlank()) {
                    continue;
                }
                Author author = authorRepository
                        .findByNameIgnoreCase(authorName.trim())
                        .orElseGet(() -> {
                            Author newAuthor =
                                    Author.builder().name(authorName.trim()).build();
                            return authorRepository.save(newAuthor);
                        });
                authors.add(BookAuthorInfo.builder()
                        .authorId(author.getId())
                        .name(author.getName())
                        .role(AuthorRole.MAIN_AUTHOR)
                        .build());
            }
        }
        // BookCreateRequest.authors đòi @Size(min=1) - Google Books trả về nhiều volume không có
        // authors (đặc biệt sách công cộng/scan cũ), fallback "Unknown Author" giống hệt cách
        // resolvePublisher() đã làm cho publisher, tránh 400 BOOK_AUTHORS_REQUIRED khi import thật.
        if (authors.isEmpty()) {
            Author unknown = authorRepository
                    .findByNameIgnoreCase(UNKNOWN_AUTHOR)
                    .orElseGet(() -> authorRepository.save(
                            Author.builder().name(UNKNOWN_AUTHOR).build()));
            authors.add(BookAuthorInfo.builder()
                    .authorId(unknown.getId())
                    .name(unknown.getName())
                    .role(AuthorRole.MAIN_AUTHOR)
                    .build());
        }
        return authors;
    }

    private List<BookCategoryInfo> resolveCategories(List<String> categoryNames) {
        List<BookCategoryInfo> categories = new ArrayList<>();
        if (categoryNames != null) {
            for (String categoryName : categoryNames) {
                if (categoryName == null || categoryName.isBlank()) {
                    continue;
                }
                Category category = categoryRepository
                        .findByNameIgnoreCase(categoryName.trim())
                        .orElseGet(() -> {
                            Category newCategory =
                                    Category.builder().name(categoryName.trim()).build();
                            return categoryRepository.save(newCategory);
                        });
                categories.add(BookCategoryInfo.builder()
                        .categoryId(category.getId())
                        .name(category.getName())
                        .slug(category.getSlug())
                        .build());
            }
        }
        // BookCreateRequest.category đòi @Size(min=1) - rất nhiều volume Google Books không có
        // categories, fallback "Unknown" giống hệt publisher, tránh 400 BOOK_CATEGORY_REQUIRED.
        if (categories.isEmpty()) {
            Category unknown = categoryRepository
                    .findByNameIgnoreCase(UNKNOWN_CATEGORY)
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder().name(UNKNOWN_CATEGORY).build()));
            categories.add(BookCategoryInfo.builder()
                    .categoryId(unknown.getId())
                    .name(unknown.getName())
                    .slug(unknown.getSlug())
                    .build());
        }
        return categories;
    }

    private BookPublisherInfo resolvePublisher(String publisherName) {
        String name = (publisherName == null || publisherName.isBlank()) ? UNKNOWN_PUBLISHER : publisherName.trim();
        Publisher publisher = publisherRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Publisher newPublisher = Publisher.builder().name(name).build();
            return publisherRepository.save(newPublisher);
        });
        return BookPublisherInfo.builder()
                .publisherId(publisher.getId())
                .name(publisher.getName())
                .build();
    }

    private String extractIsbn13(List<IndustryIdentifier> industryIdentifiers) {
        if (industryIdentifiers == null) {
            return null;
        }
        for (IndustryIdentifier identifier : industryIdentifiers) {
            if (identifier == null || identifier.getIdentifier() == null) {
                continue;
            }
            if ("ISBN_13".equalsIgnoreCase(identifier.getType())
                    && identifier.getIdentifier().matches("\\d{13}")) {
                return identifier.getIdentifier();
            }
        }
        return null;
    }

    private GoogleBooksAccessInfo mapAccessInfo(GoogleBookAccessInfo accessInfo) {
        if (accessInfo == null) {
            return null;
        }
        Boolean epubAvailable =
                accessInfo.getEpub() != null ? accessInfo.getEpub().getIsAvailable() : null;
        Boolean pdfAvailable = accessInfo.getPdf() != null ? accessInfo.getPdf().getIsAvailable() : null;

        return GoogleBooksAccessInfo.builder()
                .country(accessInfo.getCountry())
                .viewability(parseViewability(accessInfo.getViewability()))
                .embeddable(accessInfo.getEmbeddable())
                .publicDomain(accessInfo.getPublicDomain())
                .webReaderLink(accessInfo.getWebReaderLink())
                .pdfAvailable(pdfAvailable)
                .epubAvailable(epubAvailable)
                .build();
    }

    private BookViewability parseViewability(String raw) {
        if (raw == null) {
            return BookViewability.UNKNOWN;
        }
        try {
            return BookViewability.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return BookViewability.UNKNOWN;
        }
    }
}
