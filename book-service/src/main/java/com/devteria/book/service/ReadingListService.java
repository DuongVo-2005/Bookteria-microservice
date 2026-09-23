package com.devteria.book.service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.book.dto.OutboxEventType;
import com.devteria.book.dto.ReadingStatus;
import com.devteria.book.dto.ShelfPrivacy;
import com.devteria.book.dto.WrapUpPeriod;
import com.devteria.book.dto.request.ReadingChallengeUpsertRequest;
import com.devteria.book.dto.request.ReadingListCreateRequest;
import com.devteria.book.dto.request.ReadingListUpdateRequest;
import com.devteria.book.dto.request.ShelfPrivacyUpdateRequest;
import com.devteria.book.dto.response.MonthlyReadingCount;
import com.devteria.book.dto.response.PageResponse;
import com.devteria.book.dto.response.ReadingChallengeResponse;
import com.devteria.book.dto.response.ReadingListResponse;
import com.devteria.book.dto.response.ReadingStatsResponse;
import com.devteria.book.dto.response.ReadingWrapUpResponse;
import com.devteria.book.dto.response.ShelfPrivacyResponse;
import com.devteria.book.entity.Book;
import com.devteria.book.entity.BookStats;
import com.devteria.book.entity.ReadingChallenge;
import com.devteria.book.entity.ReadingList;
import com.devteria.book.entity.ShelfSettings;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.mapper.ReadingListMapper;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.ReadingChallengeRepository;
import com.devteria.book.repository.ReadingListRepository;
import com.devteria.book.repository.ShelfSettingsRepository;
import com.devteria.book.repository.httpclient.FriendClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReadingListService {
    ReadingListRepository readingListRepository;

    ReadingListMapper readingListMapper;

    BookRepository bookRepository;

    OutboxEventService outboxEventService;

    ReadingChallengeRepository readingChallengeRepository;

    ShelfSettingsRepository shelfSettingsRepository;

    FriendClient friendClient;

    public ReadingListResponse addToShelf(String bookId, ReadingListCreateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!bookRepository.existsById(bookId)) {
            throw new AppException(ErrorCode.BOOK_NOT_FOUND);
        }
        if (readingListRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new AppException(ErrorCode.READING_LIST_ALREADY_EXISTS);
        }
        ReadingStatus status = request.getStatus();
        if (status == null) {
            status = ReadingStatus.WANT_TO_READ;
        }
        ReadingList.ReadingListBuilder builder = ReadingList.builder()
                .bookId(bookId)
                .userId(userId)
                .status(status)
                .createdAt(Instant.now());

        // Bug thật bắt được lúc live-verify: thêm thẳng sách vào shelf với status READING/
        // COMPLETED (vd backlog sách đã đọc xong từ trước) trước đây KHÔNG set startedAt/
        // completedAt (2 field đó trước đây chỉ được set khi CHUYỂN trạng thái qua
        // updateProgress()/upsertMyProgressByBookId(), addToShelf() tạo mới thẳng nên không có
        // "trạng thái cũ" nào để so sánh) - khiến sách COMPLETED ngay từ lúc thêm bị Reading
        // Wrap-up/Reading Stats monthly breakdown/Annual Reading Challenge bỏ sót hoàn toàn (đều
        // lọc theo completedAt != null).
        Instant now = Instant.now();
        if (status == ReadingStatus.READING) {
            builder.startedAt(now);
        } else if (status == ReadingStatus.COMPLETED) {
            builder.completedAt(now);
        }

        ReadingList readingList = builder.build();
        readingListRepository.save(readingList);

        if (status == ReadingStatus.COMPLETED) {
            incrementChallengeProgress(userId, readingList.getCompletedAt());
        }

        recordProgressEvent(readingList);
        return readingListMapper.toReadingListResponse(readingList);
    }

    public ReadingListResponse updateProgress(String id, ReadingListUpdateRequest request) {
        ReadingList readingList = readingListRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.READING_LIST_NOT_FOUND));

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!readingList.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        ReadingStatus oldStatus = readingList.getStatus();
        if (request.getStatus() != null) {
            ReadingStatus newStatus = request.getStatus();
            if (newStatus == ReadingStatus.READING && oldStatus != ReadingStatus.READING) {
                if (readingList.getStartedAt() == null) {
                    readingList.setStartedAt(Instant.now());
                }
            }
            if (newStatus == ReadingStatus.COMPLETED && oldStatus != ReadingStatus.COMPLETED) {
                readingList.setCompletedAt(Instant.now());
                incrementChallengeProgress(userId, readingList.getCompletedAt());
            }
            readingList.setStatus(newStatus);
        }
        if (request.getCurrentPage() != null) {
            Integer currentPage = request.getCurrentPage();
            if (currentPage < 0) {
                throw new AppException(ErrorCode.READING_LIST_PAGE_INVALID);
            }
            Book book = bookRepository
                    .findById(readingList.getBookId())
                    .orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
            if (book.getMetadata() != null
                    && book.getMetadata().getPageCount() != null
                    && currentPage > book.getMetadata().getPageCount()) {
                throw new AppException(ErrorCode.READING_LIST_PAGE_INVALID);
            }
            readingList.setCurrentPage(currentPage);
        }
        if (request.getProgressPercent() != null) {
            readingList.setProgressPercent(request.getProgressPercent());
        }
        if (request.getNotes() != null) {
            readingList.setNotes(request.getNotes());
        }
        readingList.setUpdatedAt(Instant.now());

        readingListRepository.save(readingList);
        recordProgressEvent(readingList);

        return readingListMapper.toReadingListResponse(readingList);
    }

    public ReadingListResponse getMyProgressByBookId(String bookId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        ReadingList readingList = readingListRepository.findByUserIdAndBookId(userId, bookId);
        if (readingList == null) {
            return ReadingListResponse.builder()
                    .bookId(bookId)
                    .status(ReadingStatus.WANT_TO_READ)
                    .currentPage(0)
                    .progressPercent(0)
                    .build();
        }
        return readingListMapper.toReadingListResponse(readingList);
    }

    public ReadingListResponse upsertMyProgressByBookId(String bookId, ReadingListUpdateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        ReadingList readingList = readingListRepository.findByUserIdAndBookId(userId, bookId);

        if (readingList == null) {
            if (!bookRepository.existsById(bookId)) {
                throw new AppException(ErrorCode.BOOK_NOT_FOUND);
            }
            readingList = ReadingList.builder()
                    .userId(userId)
                    .bookId(bookId)
                    .status(ReadingStatus.READING)
                    .startedAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();
        }

        ReadingStatus oldStatus = readingList.getStatus();
        if (request.getStatus() != null) {
            ReadingStatus newStatus = request.getStatus();
            if (newStatus == ReadingStatus.READING
                    && oldStatus != ReadingStatus.READING
                    && readingList.getStartedAt() == null) {
                readingList.setStartedAt(Instant.now());
            }
            if (newStatus == ReadingStatus.COMPLETED && oldStatus != ReadingStatus.COMPLETED) {
                readingList.setCompletedAt(Instant.now());
                incrementChallengeProgress(userId, readingList.getCompletedAt());
            }
            readingList.setStatus(newStatus);
        }
        if (request.getCurrentPage() != null) {
            Integer currentPage = request.getCurrentPage();
            if (currentPage < 0) {
                throw new AppException(ErrorCode.READING_LIST_PAGE_INVALID);
            }
            Book book = bookRepository.findById(bookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
            if (book.getMetadata() != null
                    && book.getMetadata().getPageCount() != null
                    && currentPage > book.getMetadata().getPageCount()) {
                throw new AppException(ErrorCode.READING_LIST_PAGE_INVALID);
            }
            readingList.setCurrentPage(currentPage);
        }
        if (request.getProgressPercent() != null) {
            readingList.setProgressPercent(request.getProgressPercent());
        }
        if (request.getNotes() != null) {
            readingList.setNotes(request.getNotes());
        }
        readingList.setUpdatedAt(Instant.now());

        readingListRepository.save(readingList);
        recordProgressEvent(readingList);

        return readingListMapper.toReadingListResponse(readingList);
    }

    public PageResponse<ReadingListResponse> getMyReadingList(int page, int size, ReadingStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        var pageData = status == null
                ? readingListRepository.findAllByUserId(userId, pageable)
                : readingListRepository.findAllByUserIdAndStatus(userId, status, pageable);
        var bookList =
                pageData.stream().map(readingListMapper::toReadingListResponse).toList();

        return PageResponse.<ReadingListResponse>builder()
                .data(bookList)
                .currentPage(page)
                .pageSize(size)
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .build();
    }

    // idea-spec BA v2 §4.3: xem Kệ sách của NGƯỜI KHÁC, có kiểm tra quyền riêng tư - đối xứng với
    // getMyReadingList() ở trên nhưng cho targetUserId thay vì user đang đăng nhập.
    public PageResponse<ReadingListResponse> getUserReadingList(
            String targetUserId, int page, int size, ReadingStatus status) {
        String callerId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!callerId.equals(targetUserId) && !isAdmin()) {
            checkShelfAccess(callerId, targetUserId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var pageData = status == null
                ? readingListRepository.findAllByUserId(targetUserId, pageable)
                : readingListRepository.findAllByUserIdAndStatus(targetUserId, status, pageable);
        var bookList =
                pageData.stream().map(readingListMapper::toReadingListResponse).toList();

        return PageResponse.<ReadingListResponse>builder()
                .data(bookList)
                .currentPage(page)
                .pageSize(size)
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .build();
    }

    // Thiếu ShelfSettings = mặc định PUBLIC (chưa từng cài đặt gì).
    private void checkShelfAccess(String callerId, String targetUserId) {
        ShelfPrivacy privacy = shelfSettingsRepository
                .findById(targetUserId)
                .map(ShelfSettings::getPrivacy)
                .orElse(ShelfPrivacy.PUBLIC);

        if (privacy == ShelfPrivacy.PUBLIC) {
            return;
        }
        if (privacy == ShelfPrivacy.PRIVATE) {
            throw new AppException(ErrorCode.SHELF_ACCESS_DENIED);
        }
        // FRIENDS_ONLY
        var friendsResponse = friendClient.getFriendUserIds(targetUserId);
        var friendIds = friendsResponse != null ? friendsResponse.getResult() : null;
        if (friendIds == null || !friendIds.contains(callerId)) {
            throw new AppException(ErrorCode.SHELF_ACCESS_DENIED);
        }
    }

    public ShelfPrivacyResponse getMyShelfPrivacy() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        ShelfPrivacy privacy = shelfSettingsRepository
                .findById(userId)
                .map(ShelfSettings::getPrivacy)
                .orElse(ShelfPrivacy.PUBLIC);
        return ShelfPrivacyResponse.builder().privacy(privacy).build();
    }

    public ShelfPrivacyResponse updateMyShelfPrivacy(ShelfPrivacyUpdateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        ShelfSettings settings = shelfSettingsRepository
                .findById(userId)
                .orElseGet(() -> ShelfSettings.builder().userId(userId).build());
        settings.setPrivacy(request.getPrivacy());
        settings.setUpdatedAt(Instant.now());
        shelfSettingsRepository.save(settings);
        return ShelfPrivacyResponse.builder().privacy(settings.getPrivacy()).build();
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    // idea-spec Phase 5 - "17. Reading Statistics". Gộp theo tháng trong Java (không dùng Mongo
    // aggregation pipeline) - reading list của 1 user không lớn (vài chục tới vài trăm cuốn),
    // group-by trong bộ nhớ đơn giản hơn và không cần verify cú pháp aggregation lúc không có
    // Mongo thật để test.
    public ReadingStatsResponse getMyStats() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        long wantToRead = readingListRepository.countByUserIdAndStatus(userId, ReadingStatus.WANT_TO_READ);
        long reading = readingListRepository.countByUserIdAndStatus(userId, ReadingStatus.READING);
        long completed = readingListRepository.countByUserIdAndStatus(userId, ReadingStatus.COMPLETED);

        List<ReadingList> completedEntries =
                readingListRepository.findAllByUserIdAndStatus(userId, ReadingStatus.COMPLETED);
        Map<String, Long> countByMonth = completedEntries.stream()
                .filter(entry -> entry.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        entry -> YearMonth.from(entry.getCompletedAt().atZone(ZoneOffset.UTC))
                                .toString(),
                        Collectors.counting()));

        List<MonthlyReadingCount> completedByMonth = new ArrayList<>();
        YearMonth cursor = YearMonth.now(ZoneOffset.UTC).minusMonths(11);
        for (int i = 0; i < 12; i++) {
            String key = cursor.toString();
            completedByMonth.add(MonthlyReadingCount.builder()
                    .month(key)
                    .count(countByMonth.getOrDefault(key, 0L))
                    .build());
            cursor = cursor.plusMonths(1);
        }

        return ReadingStatsResponse.builder()
                .booksWantToRead(wantToRead)
                .booksReading(reading)
                .booksCompleted(completed)
                .totalBooks(wantToRead + reading + completed)
                .completedByMonth(completedByMonth)
                .build();
    }

    private static final int WRAP_UP_TOP_LIMIT = 3;

    // idea-spec BA v2 §4.2: Reading Wrap-up - dữ liệu để FE tự vẽ "Stats Card" theo tuần/tháng/
    // năm, chia sẻ lên Feed/mạng xã hội (canvas rendering là việc của FE, giống hệt quyết định
    // phạm vi đã áp dụng cho FEAT-04 Quote Card). Không tính "tổng số trang/thời gian đọc" vì dự
    // án chưa có bảng ReadingSession/durationSeconds (đã ghi rõ ở FEAT-02's ReadingStreak) - chỉ
    // tính những gì có dữ liệu thật: số sách hoàn thành + thể loại/tác giả đọc nhiều nhất.
    public ReadingWrapUpResponse getMyWrapUp(WrapUpPeriod period) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Instant to = Instant.now();
        Instant from = to.minus(period.getDays(), java.time.temporal.ChronoUnit.DAYS);

        List<ReadingList> completedInPeriod =
                readingListRepository.findAllByUserIdAndStatus(userId, ReadingStatus.COMPLETED).stream()
                        .filter(entry -> entry.getCompletedAt() != null
                                && !entry.getCompletedAt().isBefore(from))
                        .toList();

        List<String> bookIds = completedInPeriod.stream()
                .map(ReadingList::getBookId)
                .distinct()
                .toList();
        List<Book> books = bookRepository.findAllById(bookIds);

        Map<String, Long> categoryFrequency = books.stream()
                .filter(book -> book.getCategories() != null)
                .flatMap(book -> book.getCategories().stream())
                .collect(Collectors.groupingBy(
                        com.devteria.book.entity.BookCategoryInfo::getName, Collectors.counting()));
        Map<String, Long> authorFrequency = books.stream()
                .filter(book -> book.getAuthors() != null)
                .flatMap(book -> book.getAuthors().stream())
                .collect(
                        Collectors.groupingBy(com.devteria.book.entity.BookAuthorInfo::getName, Collectors.counting()));

        return ReadingWrapUpResponse.builder()
                .period(period)
                .from(from)
                .to(to)
                .booksCompleted(completedInPeriod.size())
                .topCategories(topKeys(categoryFrequency))
                .topAuthors(topKeys(authorFrequency))
                .build();
    }

    private List<String> topKeys(Map<String, Long> frequency) {
        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(WRAP_UP_TOP_LIMIT)
                .map(Map.Entry::getKey)
                .toList();
    }

    public void removeFromShelf(String id) {
        ReadingList readingList = readingListRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.READING_LIST_NOT_FOUND));
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!readingList.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        readingListRepository.delete(readingList);
        recordProgressEvent(readingList);
    }

    // Đồng bộ từ reading-service (Kafka, topic "reading-progress-events") — KHÔNG gọi
    // recordProgressEvent() ở đây, tránh phát lại event READING_PROGRESS_UPDATED (hiện chưa có
    // consumer nào nghe, nhưng đây là hành động hệ thống tự động, không phải user chủ động sửa
    // shelf, giữ ngữ nghĩa event đó đúng như thiết kế ban đầu).
    public void syncStatusFromReadingProgress(String userId, String bookId, ReadingStatus newStatus) {
        ReadingList readingList = readingListRepository.findByUserIdAndBookId(userId, bookId);
        if (readingList == null) {
            readingList = ReadingList.builder()
                    .userId(userId)
                    .bookId(bookId)
                    .createdAt(Instant.now())
                    .build();
        }

        ReadingStatus oldStatus = readingList.getStatus();
        if (newStatus == ReadingStatus.READING
                && oldStatus != ReadingStatus.READING
                && readingList.getStartedAt() == null) {
            readingList.setStartedAt(Instant.now());
        }
        if (newStatus == ReadingStatus.COMPLETED && oldStatus != ReadingStatus.COMPLETED) {
            readingList.setCompletedAt(Instant.now());
            incrementChallengeProgress(userId, readingList.getCompletedAt());
        }
        readingList.setStatus(newStatus);
        readingList.setUpdatedAt(Instant.now());

        readingListRepository.save(readingList);
    }

    // idea-spec Phase 6 - "19. Trending Books": Book.stats.readCount/wantToReadCount tồn tại sẵn
    // trong entity (từ trước) nhưng chưa từng được set ở đâu cả - luôn bằng null/0. Job này lần
    // đầu wire số liệu thật vào 2 field đó. Tính bằng cách quét toàn bộ reading_lists 1 lần rồi
    // group trong Java (không dùng Mongo aggregation pipeline - không có Mongo thật để verify cú
    // pháp trong phiên code này, xem lý do tương tự ở getMyStats()). Chạy 1 lần/ngày, không cần
    // realtime cho mục đích "trending".
    @Scheduled(cron = "0 30 3 * * *")
    public void refreshBookPopularityStats() {
        List<ReadingList> all = readingListRepository.findAll();

        Map<String, Long> readCounts = all.stream()
                .filter(rl -> rl.getStatus() == ReadingStatus.READING || rl.getStatus() == ReadingStatus.COMPLETED)
                .collect(Collectors.groupingBy(ReadingList::getBookId, Collectors.counting()));
        Map<String, Long> wantToReadCounts = all.stream()
                .filter(rl -> rl.getStatus() == ReadingStatus.WANT_TO_READ)
                .collect(Collectors.groupingBy(ReadingList::getBookId, Collectors.counting()));

        Set<String> bookIds = new HashSet<>();
        bookIds.addAll(readCounts.keySet());
        bookIds.addAll(wantToReadCounts.keySet());

        for (String bookId : bookIds) {
            bookRepository.findById(bookId).ifPresent(book -> {
                BookStats stats = book.getStats() != null
                        ? book.getStats()
                        : BookStats.builder().build();
                stats.setReadCount(readCounts.getOrDefault(bookId, 0L));
                stats.setWantToReadCount(wantToReadCounts.getOrDefault(bookId, 0L));
                book.setStats(stats);
                bookRepository.save(book);
            });
        }
    }

    // idea-spec BA FEAT-02: Annual Reading Challenge
    public ReadingChallengeResponse setMyReadingChallenge(ReadingChallengeUpsertRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        ReadingChallenge challenge = readingChallengeRepository
                .findByUserIdAndYear(userId, request.getYear())
                .orElseGet(() -> ReadingChallenge.builder()
                        .userId(userId)
                        .year(request.getYear())
                        .completedBooks(0)
                        .createdAt(Instant.now())
                        .build());
        challenge.setTargetBooks(request.getTargetBooks());
        challenge.setUpdatedAt(Instant.now());
        challenge = readingChallengeRepository.save(challenge);

        return toReadingChallengeResponse(challenge);
    }

    public ReadingChallengeResponse getMyReadingChallenge(int year) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        return readingChallengeRepository
                .findByUserIdAndYear(userId, year)
                .map(this::toReadingChallengeResponse)
                .orElseGet(() -> ReadingChallengeResponse.builder()
                        .year(year)
                        .targetBooks(0)
                        .completedBooks(0)
                        .percent(0)
                        .build());
    }

    private ReadingChallengeResponse toReadingChallengeResponse(ReadingChallenge challenge) {
        double percent = challenge.getTargetBooks() <= 0
                ? 0
                : Math.min(100.0, (challenge.getCompletedBooks() * 100.0) / challenge.getTargetBooks());
        return ReadingChallengeResponse.builder()
                .year(challenge.getYear())
                .targetBooks(challenge.getTargetBooks())
                .completedBooks(challenge.getCompletedBooks())
                .percent(percent)
                .build();
    }

    // Gọi ngay khi 1 ReadingList chuyển sang COMPLETED (3 nơi: updateProgress,
    // upsertMyProgressByBookId, syncStatusFromReadingProgress) - tăng đúng challenge của NĂM sách
    // được hoàn thành (theo completedAt), không phải năm hiện tại lúc gọi (khác biệt chỉ xảy ra khi
    // hoàn thành sách đúng lúc giao thời năm, chấp nhận được - completedAt luôn = Instant.now() tại
    // đúng thời điểm gọi nên 2 giá trị này trên thực tế luôn trùng nhau).
    private void incrementChallengeProgress(String userId, Instant completedAt) {
        int year = completedAt.atZone(ZoneOffset.UTC).getYear();
        readingChallengeRepository.findByUserIdAndYear(userId, year).ifPresent(challenge -> {
            challenge.setCompletedBooks(challenge.getCompletedBooks() + 1);
            challenge.setUpdatedAt(Instant.now());
            readingChallengeRepository.save(challenge);
        });
    }

    private void recordProgressEvent(ReadingList readingList) {
        var payload = Map.of(
                "userId",
                readingList.getUserId(),
                "bookId",
                readingList.getBookId(),
                "status",
                readingList.getStatus(),
                "currentPage",
                readingList.getCurrentPage() == null ? 0 : readingList.getCurrentPage());
        outboxEventService.recordEvent(readingList.getBookId(), OutboxEventType.READING_PROGRESS_UPDATED, payload);
    }
}
