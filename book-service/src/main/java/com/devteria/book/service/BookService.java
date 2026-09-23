package com.devteria.book.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.devteria.book.dto.BookStatus;
import com.devteria.book.dto.BookViewability;
import com.devteria.book.dto.OutboxEventType;
import com.devteria.book.dto.ReadingStatus;
import com.devteria.book.dto.request.BookBatchImportRequest;
import com.devteria.book.dto.request.BookCreateRequest;
import com.devteria.book.dto.response.BatchImportResponse;
import com.devteria.book.dto.response.BookResponse;
import com.devteria.book.dto.response.PageResponse;
import com.devteria.book.dto.response.ReadingAccessResponse;
import com.devteria.book.entity.Book;
import com.devteria.book.entity.BookAuthorInfo;
import com.devteria.book.entity.BookCategoryInfo;
import com.devteria.book.entity.BookStats;
import com.devteria.book.entity.GoogleBooksAccessInfo;
import com.devteria.book.entity.ReadingList;
import com.devteria.book.entity.Review;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.exception.error.BatchImportError;
import com.devteria.book.mapper.BookMapper;
import com.devteria.book.repository.AuthorRepository;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.CategoryRepository;
import com.devteria.book.repository.PublisherRepository;
import com.devteria.book.repository.ReadingListRepository;
import com.devteria.book.repository.ReviewRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookService {
    BookRepository bookRepository;

    AuthorRepository authorRepository;

    CategoryRepository categoryRepository;

    PublisherRepository publisherRepository;

    BookMapper bookMapper;

    Validator validator;

    private static final String BOOK_DETAIL_CACHE_PREFIX = "book:detail:";

    private static final String BOOK_SLUG_CACHE_PREFIX = "book:slug:";

    private static final Duration BOOK_CACHE_TTL = Duration.ofHours(12);

    RedisTemplate<String, BookResponse> bookRedisTemplate;

    OutboxEventService outboxEventService;

    ReadingListRepository readingListRepository;

    ReviewRepository reviewRepository;

    private void validateReferences(BookCreateRequest request) {
        for (BookAuthorInfo author : request.getAuthors()) {
            if (!authorRepository.existsById(author.getAuthorId())) {
                throw new AppException(ErrorCode.AUTHOR_NOT_FOUND);
            }
        }
        for (BookCategoryInfo category : request.getCategory()) {
            if (!categoryRepository.existsById(category.getCategoryId())) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }
        if (!publisherRepository.existsById(request.getPublishers().getPublisherId())) {
            throw new AppException(ErrorCode.PUBLISHER_NOT_FOUND);
        }
    }

    public BookResponse createBook(BookCreateRequest request) {
        if (request.getIsbn13() != null && bookRepository.existsByIsbn13(request.getIsbn13())) {
            throw new AppException(ErrorCode.BOOK_ISBN13_ALREADY_EXISTS);
        }
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = request.getTitle().toLowerCase().replaceAll(" ", "-");
        }
        if (slug != null && bookRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.BOOK_SLUG_ALREADY_EXISTS);
        }
        validateReferences(request);
        Book book = Book.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .slug(slug)
                .isbn13(request.getIsbn13())
                .description(request.getDescription())
                .authors(request.getAuthors())
                .categories(request.getCategory())
                .publisher(request.getPublishers())
                .metadata(request.getMetadata())
                .status(request.getStatus() != null ? request.getStatus() : BookStatus.DRAFT)
                .stats(request.getStats())
                .createdAt(Instant.now())
                .googleBookId(request.getGoogleBookId())
                .googleBooksAccess(request.getGoogleBooksAccess())
                .build();
        bookRepository.save(book);
        outboxEventService.recordEvent(book.getId(), OutboxEventType.BOOK_CREATED, bookMapper.toBookResponse(book));
        return bookMapper.toBookResponse(book);
    }

    public BatchImportResponse batchImportBooks(BookBatchImportRequest request) {
        int success = 0;
        int failed = 0;
        List<BatchImportError> errors = new ArrayList<>();
        if (request == null
                || request.getBookCreateRequests() == null
                || request.getBookCreateRequests().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        List<BookCreateRequest> books = request.getBookCreateRequests();
        for (int i = 0; i < books.size(); i++) {
            BookCreateRequest item = books.get(i);
            Set<ConstraintViolation<BookCreateRequest>> violations = validator.validate(item);
            if (!violations.isEmpty()) {
                failed++;
                String message = violations.stream()
                        .map(validation -> {
                            try {
                                return ErrorCode.valueOf(validation.getMessage())
                                        .getMessage();
                            } catch (IllegalArgumentException e) {
                                return validation.getMessage();
                            }
                        })
                        .collect(Collectors.joining(", "));

                errors.add(BatchImportError.builder().index(i).message(message).build());
                continue;
            }
            try {
                createBook(item);
                success++;
            } catch (AppException exception) {
                failed++;
                errors.add(BatchImportError.builder()
                        .index(i)
                        .message(exception.getMessage())
                        .build());
            } catch (Exception e) {
                failed++;
                errors.add(BatchImportError.builder()
                        .index(i)
                        .message("Unexpected error: " + e.getClass().getSimpleName())
                        .build());

                log.warn("Batch import failed at index {}", i, e);
            }
        }
        return BatchImportResponse.builder()
                .success(success)
                .failed(failed)
                .errors(errors)
                .build();
    }

    public BookResponse updateBook(String bookId, BookCreateRequest request) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));

        String oldSlug = book.getSlug();

        if (request.getIsbn13() != null
                && !request.getIsbn13().equals(book.getIsbn13())
                && bookRepository.existsByIsbn13(request.getIsbn13())) {
            throw new AppException(ErrorCode.BOOK_ISBN13_ALREADY_EXISTS);
        }
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = request.getTitle().toLowerCase().replaceAll(" ", "-");
        }
        if (slug != null && !slug.equals(book.getSlug()) && bookRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.BOOK_SLUG_ALREADY_EXISTS);
        }
        validateReferences(request);
        book.setTitle(request.getTitle());
        book.setSubtitle(request.getSubtitle());
        book.setSlug(slug);
        book.setIsbn13(request.getIsbn13());
        book.setDescription(request.getDescription());
        book.setAuthors(request.getAuthors());
        book.setCategories(request.getCategory());
        book.setPublisher(request.getPublishers());
        book.setMetadata(request.getMetadata());
        book.setStats(request.getStats());
        if (request.getStatus() != null) {
            book.setStatus(request.getStatus());
        }
        book.setUpdatedAt(Instant.now());
        bookRepository.save(book);
        outboxEventService.recordEvent(book.getId(), OutboxEventType.BOOK_UPDATED, bookMapper.toBookResponse(book));

        evictBookCache(bookId, oldSlug);

        if (slug != null && !slug.equals(oldSlug)) {
            bookRedisTemplate.delete(BOOK_SLUG_CACHE_PREFIX + slug);
        }
        return bookMapper.toBookResponse(book);
    }

    public BookResponse getBookById(String bookId) {
        String cacheKey = BOOK_DETAIL_CACHE_PREFIX + bookId;
        BookResponse cacheBook = bookRedisTemplate.opsForValue().get(cacheKey);

        if (cacheBook != null) {
            return cacheBook;
        }
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));

        BookResponse response = bookMapper.toBookResponse(book);

        bookRedisTemplate.opsForValue().set(cacheKey, response, BOOK_CACHE_TTL);

        return response;
    }

    public BookResponse getBookBySlug(String slug) {
        String cacheKey = BOOK_SLUG_CACHE_PREFIX + slug;
        BookResponse cacheBook = bookRedisTemplate.opsForValue().get(cacheKey);

        if (cacheBook != null) {
            return cacheBook;
        }
        Book book = bookRepository.findBySlug(slug).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));

        BookResponse response = bookMapper.toBookResponse(book);

        bookRedisTemplate.opsForValue().set(cacheKey, response, BOOK_CACHE_TTL);

        return response;
    }

    public ReadingAccessResponse getReadingAccess(String bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
        GoogleBooksAccessInfo access = book.getGoogleBooksAccess();

        if (access == null || access.getViewability() == null) {
            return ReadingAccessResponse.builder()
                    .bookId(book.getId())
                    .googleBookId(book.getGoogleBookId())
                    .canRead(false)
                    .build();
        }

        boolean readableViewability = access.getViewability() == BookViewability.ALL_PAGES
                || access.getViewability() == BookViewability.PARTIAL;
        boolean hasWebReaderLink =
                access.getWebReaderLink() != null && !access.getWebReaderLink().isBlank();
        boolean canRead = readableViewability && (Boolean.TRUE.equals(access.getEmbeddable()) || hasWebReaderLink);

        return ReadingAccessResponse.builder()
                .bookId(book.getId())
                .googleBookId(book.getGoogleBookId())
                .canRead(canRead)
                .viewability(access.getViewability())
                .embeddable(access.getEmbeddable())
                .publicDomain(access.getPublicDomain())
                .webReaderLink(access.getWebReaderLink())
                .build();
    }

    public List<BookResponse> getBookByCategoryId(String categoryId) {
        return bookRepository.findAllByCategories_CategoryId(categoryId).stream()
                .map(bookMapper::toBookResponse)
                .toList();
    }

    public List<BookResponse> getBookByAuthorId(String authorId) {
        return bookRepository.findAllByAuthors_AuthorId(authorId).stream()
                .map(bookMapper::toBookResponse)
                .toList();
    }

    public void deleteBook(String bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
        bookRepository.delete(book);
        evictBookCache(bookId, book.getSlug());

        // idea-spec BA GAP-02: không xoá ReadingList của user (mất lịch sử "đã từng đọc gì") —
        // chuyển sang UNAVAILABLE để FE tự hiển thị đúng thay vì trỏ tới 1 bookId đã biến mất.
        // Đồng bộ trong Java (không Mongo bulk-update), nhất quán với cách project đã làm cho các
        // job tương tự (trending/recommendation) — quy mô reading_lists hiện tại đủ nhỏ.
        List<ReadingList> affected = readingListRepository.findAllByBookId(bookId);
        if (!affected.isEmpty()) {
            Instant now = Instant.now();
            affected.forEach(rl -> {
                rl.setStatus(ReadingStatus.UNAVAILABLE);
                rl.setUpdatedAt(now);
            });
            readingListRepository.saveAll(affected);
        }

        outboxEventService.recordEvent(bookId, OutboxEventType.BOOK_DELETED, Map.of("bookId", bookId));
    }

    // idea-spec BA v2 §2.1/P1-02: LIBRARIAN/ADMIN khoá/mở viết Review cho 1 sách (Review
    // Bombing) - review cũ giữ nguyên, chỉ chặn tạo review mới (xem ReviewService.createReview()).
    public BookResponse setReviewsLocked(String bookId, boolean locked) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
        book.setReviewsLocked(locked);
        book = bookRepository.save(book);
        evictBookCache(bookId, book.getSlug());
        return bookMapper.toBookResponse(book);
    }

    // idea-spec BA OPS-02: Merge Books Tool — Admin gộp 2 bản ghi sách trùng nhau (thường do import
    // trùng từ Google Books). Chuyển Review (kèm rating) + ReadingList từ sách trùng (B) sang sách
    // gốc (A), báo group-service re-point GroupPost.bookRef qua Kafka (BOOK_MERGED, khác
    // BOOK_DELETED vì cần CHUYỂN chứ không chỉ đánh dấu deleted), rồi xoá B bằng chính deleteBook()
    // hiện có — lúc này ReadingList của B đã rỗng nên nhánh "chuyển UNAVAILABLE" của deleteBook()
    // không còn gì để làm, và BOOK_DELETED của B vẫn kích hoạt đúng dọn dẹp Chapter/Highlight/
    // Bookmark/ReadingProgress ở reading-service + gỡ khỏi index ở search-service (tái dùng
    // nguyên consumer đã có, không cần viết thêm).
    public BookResponse mergeBooks(String targetBookId, String duplicateBookId) {
        if (targetBookId.equals(duplicateBookId)) {
            throw new AppException(ErrorCode.CANNOT_MERGE_BOOK_WITH_ITSELF);
        }
        bookRepository.findById(targetBookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
        if (!bookRepository.existsById(duplicateBookId)) {
            throw new AppException(ErrorCode.BOOK_NOT_FOUND);
        }

        mergeReviews(targetBookId, duplicateBookId);
        mergeReadingLists(targetBookId, duplicateBookId);

        outboxEventService.recordEvent(
                duplicateBookId,
                OutboxEventType.BOOK_MERGED,
                Map.of("fromBookId", duplicateBookId, "toBookId", targetBookId));

        deleteBook(duplicateBookId);

        Book mergedTarget =
                bookRepository.findById(targetBookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
        outboxEventService.recordEvent(
                targetBookId, OutboxEventType.BOOK_UPDATED, bookMapper.toBookResponse(mergedTarget));
        evictBookCache(targetBookId, mergedTarget.getSlug());

        return bookMapper.toBookResponse(mergedTarget);
    }

    private void mergeReviews(String targetBookId, String duplicateBookId) {
        List<Review> duplicateReviews = reviewRepository.findAllByBookId(duplicateBookId);
        for (Review review : duplicateReviews) {
            if (reviewRepository.existsByBookIdAndUserId(targetBookId, review.getUserId())) {
                // User đã review cả sách gốc lẫn sách trùng -> giữ review của sách gốc, bỏ bản
                // trùng (tránh cộng dồn stats 2 lần cho cùng 1 user).
                reviewRepository.delete(review);
                continue;
            }
            review.setBookId(targetBookId);
            reviewRepository.save(review);
            updateBookStatsAfterReview(targetBookId, review.getRating());
        }
    }

    private void mergeReadingLists(String targetBookId, String duplicateBookId) {
        List<ReadingList> duplicateEntries = readingListRepository.findAllByBookId(duplicateBookId);
        for (ReadingList entry : duplicateEntries) {
            if (readingListRepository.existsByUserIdAndBookId(entry.getUserId(), targetBookId)) {
                // User đã có sách gốc trên kệ -> giữ nguyên entry đó, bỏ entry trùng của sách B.
                readingListRepository.delete(entry);
                continue;
            }
            entry.setBookId(targetBookId);
            entry.setUpdatedAt(Instant.now());
            readingListRepository.save(entry);
        }
    }

    public PageResponse<BookResponse> getBooks(int page, int size, String categoryId, String authorId) {
        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id")));
        boolean hasCategory = categoryId != null && !categoryId.isBlank();
        boolean hasAuthor = authorId != null && !authorId.isBlank();

        Page<Book> pageData;
        if (hasCategory && hasAuthor) {
            pageData = bookRepository.findAllByCategories_CategoryIdAndAuthors_AuthorId(categoryId, authorId, pageable);
        } else if (hasCategory) {
            pageData = bookRepository.findAllByCategories_CategoryId(categoryId, pageable);
        } else if (hasAuthor) {
            pageData = bookRepository.findAllByAuthors_AuthorId(authorId, pageable);
        } else {
            pageData = bookRepository.findAll(pageable);
        }
        var bookList = pageData.stream().map(bookMapper::toBookResponse).toList();
        return PageResponse.<BookResponse>builder()
                .data(bookList)
                .currentPage(page)
                .pageSize(size)
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .build();
    }

    // idea-spec Phase 6 - "19. Trending Books": kết hợp readCount (đọc nhiều, weight cao nhất -
    // tín hiệu tương tác thật mạnh nhất), reviewCount (review nhiều) và ratingAverage (chất
    // lượng) thành 1 điểm trending duy nhất. Tính trong Java trên toàn bộ catalog PUBLISHED
    // (không dùng Mongo aggregation pipeline - không có Mongo thật để verify cú pháp trong phiên
    // code này) - chấp nhận được ở quy mô catalog hiện tại (mục tiêu ~1000 sách theo roadmap),
    // cần đổi sang aggregation có $lookup nếu catalog lớn hơn nhiều sau này.
    private static final double TRENDING_WEIGHT_READ = 3.0;
    private static final double TRENDING_WEIGHT_REVIEW = 2.0;
    private static final double TRENDING_WEIGHT_RATING = 5.0;

    public PageResponse<BookResponse> getTrendingBooks(int page, int size) {
        List<Book> sorted = sortedTrendingBooks(Set.of());
        return paginateBooks(sorted, page, size);
    }

    // Dùng lại bởi RecommendationService làm fallback cold-start (user mới/không đủ dữ liệu để
    // gợi ý theo category/author) - "trending" là fallback tiêu chuẩn cho bài toán cold-start.
    public List<BookResponse> getTopTrendingBooks(int limit, Set<String> excludeBookIds) {
        return sortedTrendingBooks(excludeBookIds).stream()
                .limit(limit)
                .map(bookMapper::toBookResponse)
                .toList();
    }

    private List<Book> sortedTrendingBooks(Set<String> excludeBookIds) {
        return bookRepository.findAll().stream()
                .filter(book -> book.getStatus() == BookStatus.PUBLISHED)
                .filter(book -> !excludeBookIds.contains(book.getId()))
                .sorted(java.util.Comparator.comparingDouble(this::trendingScore)
                        .reversed())
                .toList();
    }

    private double trendingScore(Book book) {
        BookStats stats = book.getStats();
        if (stats == null) {
            return 0.0;
        }
        long readCount = stats.getReadCount() == null ? 0L : stats.getReadCount();
        long reviewCount = stats.getReviewCount() == null ? 0L : stats.getReviewCount();
        double ratingAverage = stats.getRatingAverage() == null
                ? 0.0
                : stats.getRatingAverage().doubleValue();
        return readCount * TRENDING_WEIGHT_READ
                + reviewCount * TRENDING_WEIGHT_REVIEW
                + ratingAverage * TRENDING_WEIGHT_RATING;
    }

    private PageResponse<BookResponse> paginateBooks(List<Book> sorted, int page, int size) {
        int totalElements = sorted.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int from = Math.min(page * size, totalElements);
        int to = Math.min(from + size, totalElements);
        List<BookResponse> content = sorted.subList(from, to).stream()
                .map(bookMapper::toBookResponse)
                .toList();

        return PageResponse.<BookResponse>builder()
                .data(content)
                .currentPage(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    public PageResponse<BookResponse> searchBooks(String keyword, int page, int size) {
        TextCriteria criteria =
                TextCriteria.forDefaultLanguage().matchingAny(keyword.trim().split("\\s+"));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "score"));
        var pageData = bookRepository.findAllBy(criteria, pageable);
        var bookList = pageData.stream().map(bookMapper::toBookResponse).toList();
        return PageResponse.<BookResponse>builder()
                .data(bookList)
                .currentPage(page)
                .pageSize(size)
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .build();
    }

    public BookStats updateBookStatsAfterReview(String bookId, int rating) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
        BookStats stats = book.getStats();
        if (stats == null) {
            stats = BookStats.builder()
                    .reviewCount(0L)
                    .ratingCount(0L)
                    .ratingAverage(BigDecimal.ZERO)
                    .build();
        }
        long oldRatingCount = stats.getRatingCount();
        BigDecimal oldRatingAverage = stats.getRatingAverage() == null ? BigDecimal.ZERO : stats.getRatingAverage();

        long newRatingCount = oldRatingCount + 1;
        BigDecimal newRatingAverage = oldRatingAverage
                .multiply(BigDecimal.valueOf(oldRatingCount))
                .add(BigDecimal.valueOf(rating))
                .divide(BigDecimal.valueOf(newRatingCount), 2, RoundingMode.HALF_UP);
        stats.setReviewCount(stats.getReviewCount() + 1);
        stats.setRatingCount(newRatingCount);
        stats.setRatingAverage(newRatingAverage);

        book.setStats(stats);
        bookRepository.save(book);

        evictBookCache(bookId, book.getSlug());

        return stats;
    }

    public void updateBookStatsAfterReviewUpdate(String bookId, int oldRating, int newRating) {

        Book book = bookRepository.findById(bookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));

        BookStats stats = book.getStats();

        if (stats == null) {
            return;
        }

        long ratingCount = stats.getRatingCount();

        if (ratingCount <= 0) {
            return;
        }

        BigDecimal oldAverage = stats.getRatingAverage();

        if (oldAverage == null) {
            oldAverage = BigDecimal.ZERO;
        }

        BigDecimal oldTotal = oldAverage.multiply(BigDecimal.valueOf(ratingCount));

        BigDecimal newTotal = oldTotal.subtract(BigDecimal.valueOf(oldRating)).add(BigDecimal.valueOf(newRating));

        BigDecimal newAverage = newTotal.divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);

        // ratingCount KHÔNG thay đổi
        stats.setRatingAverage(newAverage);

        book.setStats(stats);

        bookRepository.save(book);

        evictBookCache(bookId, book.getSlug());
    }

    public void updateBookStatsAfterReviewDelete(String bookId, int rating) {

        Book book = bookRepository.findById(bookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));

        BookStats stats = book.getStats();

        if (stats == null) {
            return;
        }

        long oldRatingCount = stats.getRatingCount();

        if (oldRatingCount <= 0) {
            return;
        }

        long newRatingCount = oldRatingCount - 1;

        BigDecimal oldAverage = stats.getRatingAverage();

        if (oldAverage == null) {
            oldAverage = BigDecimal.ZERO;
        }

        BigDecimal newAverage;

        if (newRatingCount == 0) {
            newAverage = BigDecimal.ZERO;
        } else {
            BigDecimal oldTotal = oldAverage.multiply(BigDecimal.valueOf(oldRatingCount));

            BigDecimal newTotal = oldTotal.subtract(BigDecimal.valueOf(rating));

            newAverage = newTotal.divide(BigDecimal.valueOf(newRatingCount), 2, RoundingMode.HALF_UP);
        }

        stats.setReviewCount(Math.max(0, stats.getReviewCount() - 1));

        stats.setRatingCount(newRatingCount);

        stats.setRatingAverage(newAverage);

        book.setStats(stats);

        bookRepository.save(book);

        evictBookCache(bookId, book.getSlug());
    }

    public void evictBookCache(String bookId, String slug) {
        bookRedisTemplate.delete(BOOK_DETAIL_CACHE_PREFIX + bookId);

        if (slug != null) {
            bookRedisTemplate.delete(BOOK_SLUG_CACHE_PREFIX + slug);
        }
    }
}
