package com.devteria.book.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.book.dto.response.BookResponse;
import com.devteria.book.entity.Book;
import com.devteria.book.entity.BookAuthorInfo;
import com.devteria.book.entity.BookCategoryInfo;
import com.devteria.book.entity.ReadingList;
import com.devteria.book.entity.UserBookPreference;
import com.devteria.book.mapper.BookMapper;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.ReadingListRepository;
import com.devteria.book.repository.UserBookPreferenceRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

// idea-spec Phase 6 - "21. Book Recommendation / 21.1 Recommendation Worker/Job": tách việc
// tính recommendation khỏi request realtime, chạy Async Worker/Job định kỳ, cache Redis, ưu tiên
// đọc từ cache khi user request. Content-based đơn giản: đề xuất sách cùng category/author với
// những gì user đã đọc/đang đọc/muốn đọc, ưu tiên rating cao, loại trừ sách đã có trong shelf.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecommendationService {
    private static final String CACHE_PREFIX = "book:recommendations:";
    private static final Duration CACHE_TTL =
            Duration.ofHours(25); // > chu kỳ job 24h, tránh hết hạn ngay trước lần chạy kế tiếp
    private static final int RECOMMENDATION_LIMIT = 10;
    private static final int TOP_SIGNALS = 3; // top N category + top N author tần suất cao nhất

    ReadingListRepository readingListRepository;
    BookRepository bookRepository;
    BookMapper bookMapper;
    BookService bookService;
    StringRedisTemplate stringRedisTemplate;
    UserBookPreferenceRepository userBookPreferenceRepository;

    @Scheduled(cron = "0 0 4 * * *")
    public void refreshRecommendationsForAllUsers() {
        List<String> userIds = readingListRepository.findAll().stream()
                .map(ReadingList::getUserId)
                .distinct()
                .toList();

        for (String userId : userIds) {
            try {
                cache(userId, computeRecommendations(userId));
            } catch (Exception e) {
                log.error("Failed to refresh recommendations for userId={}", userId, e);
            }
        }
    }

    public List<BookResponse> getMyRecommendations() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        List<BookResponse> cached = readCache(userId);
        if (cached != null) {
            return cached;
        }

        List<BookResponse> computed = computeRecommendations(userId);
        cache(userId, computed);
        return computed;
    }

    private List<BookResponse> computeRecommendations(String userId) {
        List<ReadingList> myShelf = readingListRepository.findAllByUserId(userId);
        Set<String> excludeBookIds =
                myShelf.stream().map(ReadingList::getBookId).collect(Collectors.toSet());

        List<String> topCategories;
        List<String> topAuthors;

        if (!myShelf.isEmpty()) {
            List<Book> myBooks = myShelf.stream()
                    .map(entry -> bookRepository.findById(entry.getBookId()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();

            Map<String, Long> categoryFrequency = new HashMap<>();
            Map<String, Long> authorFrequency = new HashMap<>();
            for (Book book : myBooks) {
                if (book.getCategories() != null) {
                    for (BookCategoryInfo category : book.getCategories()) {
                        categoryFrequency.merge(category.getCategoryId(), 1L, Long::sum);
                    }
                }
                if (book.getAuthors() != null) {
                    for (BookAuthorInfo author : book.getAuthors()) {
                        authorFrequency.merge(author.getAuthorId(), 1L, Long::sum);
                    }
                }
            }

            topCategories = topKeys(categoryFrequency, TOP_SIGNALS);
            topAuthors = topKeys(authorFrequency, TOP_SIGNALS);
        } else {
            // idea-spec BA FEAT-01: Cold-Start Resolution — user mới chưa có ReadingList nào để suy
            // ra tín hiệu, nhưng đã khai báo sở thích lúc Onboarding Wizard thì dùng NGAY thay vì
            // rơi thẳng về trending chung chung (đúng mục tiêu "personalizedFeed ngay lập tức").
            UserBookPreference preference =
                    userBookPreferenceRepository.findById(userId).orElse(null);
            if (preference == null) {
                return bookService.getTopTrendingBooks(RECOMMENDATION_LIMIT, Set.of());
            }
            topCategories = preference.getCategoryIds() != null ? preference.getCategoryIds() : List.of();
            topAuthors = preference.getAuthorIds() != null ? preference.getAuthorIds() : List.of();
        }

        Map<String, Book> candidates = new HashMap<>();
        for (String categoryId : topCategories) {
            for (Book book : bookRepository.findAllByCategories_CategoryId(categoryId)) {
                candidates.put(book.getId(), book);
            }
        }
        for (String authorId : topAuthors) {
            for (Book book : bookRepository.findAllByAuthors_AuthorId(authorId)) {
                candidates.put(book.getId(), book);
            }
        }
        excludeBookIds.forEach(candidates::remove);

        List<BookResponse> recommendations = candidates.values().stream()
                .filter(book -> book.getStatus() == com.devteria.book.dto.BookStatus.PUBLISHED)
                .sorted(Comparator.comparing((Book book) ->
                                book.getStats() != null && book.getStats().getRatingAverage() != null
                                        ? book.getStats().getRatingAverage()
                                        : java.math.BigDecimal.ZERO)
                        .reversed())
                .limit(RECOMMENDATION_LIMIT)
                .map(bookMapper::toBookResponse)
                .toList();

        if (recommendations.isEmpty()) {
            // Không tìm được sách nào khác cùng category/author (vd user đã đọc hết) -> fallback trending.
            return bookService.getTopTrendingBooks(RECOMMENDATION_LIMIT, excludeBookIds);
        }
        return recommendations;
    }

    private List<String> topKeys(Map<String, Long> frequency, int limit) {
        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    // Cache chỉ lưu bookId (comma-joined), không lưu nguyên BookResponse JSON - tránh phụ thuộc
    // vào chi tiết API serialize/deserialize generic List của Jackson 3 (tools.jackson) trong
    // Spring Boot 4.1.1, vốn đã là nguồn gây lỗi thật nhiều lần trong project này (xem docs/
    // be-next-initiatives-tasks.md). Đọc lại bằng findAllById + map theo id để giữ đúng thứ tự.
    private void cache(String userId, List<BookResponse> recommendations) {
        try {
            String joined = recommendations.stream().map(BookResponse::getId).collect(Collectors.joining(","));
            stringRedisTemplate.opsForValue().set(CACHE_PREFIX + userId, joined, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache recommendations for userId={}", userId, e);
        }
    }

    private List<BookResponse> readCache(String userId) {
        try {
            String joined = stringRedisTemplate.opsForValue().get(CACHE_PREFIX + userId);
            if (joined == null) {
                return null;
            }
            if (joined.isBlank()) {
                return List.of();
            }
            List<String> bookIds = List.of(joined.split(","));
            Map<String, Book> byId =
                    bookRepository.findAllById(bookIds).stream().collect(Collectors.toMap(Book::getId, book -> book));
            return bookIds.stream()
                    .map(byId::get)
                    .filter(java.util.Objects::nonNull)
                    .map(bookMapper::toBookResponse)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to read cached recommendations for userId={}, recomputing", userId, e);
            return null;
        }
    }
}
