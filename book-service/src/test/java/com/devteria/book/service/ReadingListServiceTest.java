package com.devteria.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.devteria.book.dto.ReadingStatus;
import com.devteria.book.dto.ShelfPrivacy;
import com.devteria.book.dto.WrapUpPeriod;
import com.devteria.book.dto.request.ReadingListCreateRequest;
import com.devteria.book.dto.request.ReadingListUpdateRequest;
import com.devteria.book.dto.request.ShelfPrivacyUpdateRequest;
import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.ReadingListResponse;
import com.devteria.book.dto.response.ReadingWrapUpResponse;
import com.devteria.book.dto.response.ShelfPrivacyResponse;
import com.devteria.book.entity.Book;
import com.devteria.book.entity.BookAuthorInfo;
import com.devteria.book.entity.BookCategoryInfo;
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

// Unit test thuần Mockito cho ReadingListService.upsertMyProgressByBookId()/getMyProgressByBookId()
// (docs/google-books-integration-tasks.md mục 8) — trọng tâm: userId luôn lấy từ
// SecurityContextHolder, không tin bookId+userId từ client.
@ExtendWith(MockitoExtension.class)
class ReadingListServiceTest {

    private static final String USER_A = "user-A";
    private static final String BOOK_ID = "book-1";

    @Mock
    private ReadingListRepository readingListRepository;

    @Mock
    private ReadingListMapper readingListMapper;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private ReadingChallengeRepository readingChallengeRepository;

    @Mock
    private ShelfSettingsRepository shelfSettingsRepository;

    @Mock
    private FriendClient friendClient;

    private ReadingListService readingListService;

    @BeforeEach
    void setUp() {
        readingListService = new ReadingListService(
                readingListRepository,
                readingListMapper,
                bookRepository,
                outboxEventService,
                readingChallengeRepository,
                shelfSettingsRepository,
                friendClient);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(USER_A);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProgressByBookId_neverRead_returnsDefaultZeroProgress() {
        when(readingListRepository.findByUserIdAndBookId(USER_A, BOOK_ID)).thenReturn(null);

        ReadingListResponse response = readingListService.getMyProgressByBookId(BOOK_ID);

        assertThat(response.getBookId()).isEqualTo(BOOK_ID);
        assertThat(response.getStatus()).isEqualTo(ReadingStatus.WANT_TO_READ);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getProgressPercent()).isEqualTo(0);
    }

    @Test
    void upsertMyProgressByBookId_neverRead_createsNewReadingListScopedToCurrentUser() {
        when(readingListRepository.findByUserIdAndBookId(USER_A, BOOK_ID)).thenReturn(null);
        when(bookRepository.existsById(BOOK_ID)).thenReturn(true);
        when(readingListMapper.toReadingListResponse(any()))
                .thenReturn(ReadingListResponse.builder().build());

        ReadingListUpdateRequest request =
                ReadingListUpdateRequest.builder().progressPercent(10).build();

        readingListService.upsertMyProgressByBookId(BOOK_ID, request);

        ArgumentCaptor<ReadingList> captor = ArgumentCaptor.forClass(ReadingList.class);
        verify(readingListRepository).save(captor.capture());
        ReadingList saved = captor.getValue();

        // userId luôn lấy từ SecurityContextHolder, không nhận từ request/client.
        assertThat(saved.getUserId()).isEqualTo(USER_A);
        assertThat(saved.getBookId()).isEqualTo(BOOK_ID);
        assertThat(saved.getProgressPercent()).isEqualTo(10);
        assertThat(saved.getStatus()).isEqualTo(ReadingStatus.READING);
    }

    @Test
    void upsertMyProgressByBookId_alreadyExists_updatesProgressPercentInPlace() {
        ReadingList existing = ReadingList.builder()
                .id("rl-1")
                .userId(USER_A)
                .bookId(BOOK_ID)
                .status(ReadingStatus.READING)
                .progressPercent(20)
                .build();
        when(readingListRepository.findByUserIdAndBookId(USER_A, BOOK_ID)).thenReturn(existing);
        when(readingListMapper.toReadingListResponse(any()))
                .thenReturn(ReadingListResponse.builder().build());

        ReadingListUpdateRequest request =
                ReadingListUpdateRequest.builder().progressPercent(55).build();

        readingListService.upsertMyProgressByBookId(BOOK_ID, request);

        ArgumentCaptor<ReadingList> captor = ArgumentCaptor.forClass(ReadingList.class);
        verify(readingListRepository).save(captor.capture());
        assertThat(captor.getValue().getProgressPercent()).isEqualTo(55);
        assertThat(captor.getValue().getId()).isEqualTo("rl-1");
    }

    @Test
    void upsertMyProgressByBookId_userBCannotAffectUserAsProgress() {
        // findByUserIdAndBookId luôn được gọi với userId của NGƯỜI ĐANG ĐĂNG NHẬP (USER_A ở test
        // này), không phải giá trị tự truyền vào - verify đúng tham số này là cách chặn user B
        // sửa progress của user A (ownership luôn qua SecurityContextHolder, không qua request).
        when(readingListRepository.findByUserIdAndBookId(USER_A, BOOK_ID)).thenReturn(null);
        when(bookRepository.existsById(BOOK_ID)).thenReturn(true);
        when(readingListMapper.toReadingListResponse(any()))
                .thenReturn(ReadingListResponse.builder().build());

        readingListService.upsertMyProgressByBookId(
                BOOK_ID, ReadingListUpdateRequest.builder().progressPercent(5).build());

        verify(readingListRepository).findByUserIdAndBookId(USER_A, BOOK_ID);
    }

    // ---- getMyWrapUp() (BA v2 §4.2) ----

    @Test
    void getMyWrapUp_countsOnlyCompletedWithinPeriodAndRanksTopCategories() {
        java.time.Instant now = java.time.Instant.now();
        ReadingList insidePeriod1 = ReadingList.builder()
                .bookId("book-in-1")
                .status(ReadingStatus.COMPLETED)
                .completedAt(now.minus(2, java.time.temporal.ChronoUnit.DAYS))
                .build();
        ReadingList insidePeriod2 = ReadingList.builder()
                .bookId("book-in-2")
                .status(ReadingStatus.COMPLETED)
                .completedAt(now.minus(3, java.time.temporal.ChronoUnit.DAYS))
                .build();
        ReadingList outsidePeriod = ReadingList.builder()
                .bookId("book-out")
                .status(ReadingStatus.COMPLETED)
                .completedAt(now.minus(100, java.time.temporal.ChronoUnit.DAYS))
                .build();
        when(readingListRepository.findAllByUserIdAndStatus(USER_A, ReadingStatus.COMPLETED))
                .thenReturn(java.util.List.of(insidePeriod1, insidePeriod2, outsidePeriod));

        Book book1 = Book.builder()
                .id("book-in-1")
                .categories(java.util.List.of(
                        BookCategoryInfo.builder().name("Fiction").build()))
                .authors(java.util.List.of(
                        BookAuthorInfo.builder().name("Author A").build()))
                .build();
        Book book2 = Book.builder()
                .id("book-in-2")
                .categories(java.util.List.of(
                        BookCategoryInfo.builder().name("Fiction").build()))
                .authors(java.util.List.of(
                        BookAuthorInfo.builder().name("Author B").build()))
                .build();
        when(bookRepository.findAllById(any())).thenReturn(java.util.List.of(book1, book2));

        ReadingWrapUpResponse response = readingListService.getMyWrapUp(WrapUpPeriod.WEEK);

        assertThat(response.getBooksCompleted()).isEqualTo(2);
        assertThat(response.getTopCategories()).containsExactly("Fiction");
        assertThat(response.getTopAuthors()).containsExactlyInAnyOrder("Author A", "Author B");
        assertThat(response.getPeriod()).isEqualTo(WrapUpPeriod.WEEK);
    }

    // ---- Shelf Privacy (BA v2 §4.3) ----

    private static final String USER_B = "user-B";

    private Page<ReadingList> emptyPage() {
        return new PageImpl<>(List.of());
    }

    @Test
    void getUserReadingList_ownShelf_neverChecksPrivacy() {
        when(readingListRepository.findAllByUserId(any(), any())).thenReturn(emptyPage());

        readingListService.getUserReadingList(USER_A, 0, 10, null);

        verify(shelfSettingsRepository, org.mockito.Mockito.never()).findById(any());
    }

    @Test
    void getUserReadingList_targetHasNoSettings_defaultsToPublicAndAllowsStranger() {
        when(shelfSettingsRepository.findById(USER_B)).thenReturn(Optional.empty());
        when(readingListRepository.findAllByUserId(any(), any())).thenReturn(emptyPage());

        readingListService.getUserReadingList(USER_B, 0, 10, null);

        verify(readingListRepository).findAllByUserId(eq(USER_B), any());
    }

    @Test
    void getUserReadingList_targetIsPrivate_strangerIsDenied() {
        when(shelfSettingsRepository.findById(USER_B))
                .thenReturn(Optional.of(ShelfSettings.builder()
                        .userId(USER_B)
                        .privacy(ShelfPrivacy.PRIVATE)
                        .build()));

        assertThatThrownBy(() -> readingListService.getUserReadingList(USER_B, 0, 10, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHELF_ACCESS_DENIED);
    }

    @Test
    void getUserReadingList_targetIsFriendsOnly_friendIsAllowed() {
        when(shelfSettingsRepository.findById(USER_B))
                .thenReturn(Optional.of(ShelfSettings.builder()
                        .userId(USER_B)
                        .privacy(ShelfPrivacy.FRIENDS_ONLY)
                        .build()));
        when(friendClient.getFriendUserIds(USER_B))
                .thenReturn(ApiResponse.<List<String>>builder()
                        .result(List.of(USER_A))
                        .build());
        when(readingListRepository.findAllByUserId(any(), any())).thenReturn(emptyPage());

        readingListService.getUserReadingList(USER_B, 0, 10, null);

        verify(readingListRepository).findAllByUserId(eq(USER_B), any());
    }

    @Test
    void getUserReadingList_targetIsFriendsOnly_nonFriendIsDenied() {
        when(shelfSettingsRepository.findById(USER_B))
                .thenReturn(Optional.of(ShelfSettings.builder()
                        .userId(USER_B)
                        .privacy(ShelfPrivacy.FRIENDS_ONLY)
                        .build()));
        when(friendClient.getFriendUserIds(USER_B))
                .thenReturn(
                        ApiResponse.<List<String>>builder().result(List.of()).build());

        assertThatThrownBy(() -> readingListService.getUserReadingList(USER_B, 0, 10, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHELF_ACCESS_DENIED);
    }

    @Test
    void getUserReadingList_adminBypassesPrivateShelf() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        org.mockito.Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication)
                .getAuthorities();
        when(readingListRepository.findAllByUserId(any(), any())).thenReturn(emptyPage());

        readingListService.getUserReadingList(USER_B, 0, 10, null);

        verify(shelfSettingsRepository, org.mockito.Mockito.never()).findById(any());
        verify(readingListRepository).findAllByUserId(eq(USER_B), any());
    }

    @Test
    void getMyShelfPrivacy_noSettingYet_defaultsToPublic() {
        when(shelfSettingsRepository.findById(USER_A)).thenReturn(Optional.empty());

        ShelfPrivacyResponse response = readingListService.getMyShelfPrivacy();

        assertThat(response.getPrivacy()).isEqualTo(ShelfPrivacy.PUBLIC);
    }

    @Test
    void updateMyShelfPrivacy_savesSettingScopedToCurrentUser() {
        when(shelfSettingsRepository.findById(USER_A)).thenReturn(Optional.empty());

        readingListService.updateMyShelfPrivacy(ShelfPrivacyUpdateRequest.builder()
                .privacy(ShelfPrivacy.FRIENDS_ONLY)
                .build());

        ArgumentCaptor<ShelfSettings> captor = ArgumentCaptor.forClass(ShelfSettings.class);
        verify(shelfSettingsRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_A);
        assertThat(captor.getValue().getPrivacy()).isEqualTo(ShelfPrivacy.FRIENDS_ONLY);
    }

    // ---- addToShelf() startedAt/completedAt regression (bug thật bắt được lúc live-verify) ----
    // Thêm thẳng sách vào shelf với status COMPLETED/READING (vd backlog sách đã đọc xong) trước
    // đây không set startedAt/completedAt (2 field đó chỉ được set khi CHUYỂN trạng thái qua
    // update*, addToShelf() tạo mới thẳng không có "trạng thái cũ" để so sánh) - khiến Reading
    // Wrap-up/Stats/Annual Challenge bỏ sót hoàn toàn những sách này (đều lọc theo completedAt).

    @Test
    void addToShelf_statusCompleted_setsCompletedAtAndIncrementsChallenge() {
        when(bookRepository.existsById(BOOK_ID)).thenReturn(true);
        when(readingListRepository.existsByUserIdAndBookId(USER_A, BOOK_ID)).thenReturn(false);
        when(readingListMapper.toReadingListResponse(any()))
                .thenReturn(ReadingListResponse.builder().build());
        int currentYear = java.time.Year.now().getValue();
        com.devteria.book.entity.ReadingChallenge challenge = com.devteria.book.entity.ReadingChallenge.builder()
                .userId(USER_A)
                .year(currentYear)
                .targetBooks(10)
                .completedBooks(0)
                .build();
        when(readingChallengeRepository.findByUserIdAndYear(USER_A, currentYear))
                .thenReturn(Optional.of(challenge));

        ReadingListCreateRequest request = ReadingListCreateRequest.builder()
                .bookId(BOOK_ID)
                .status(ReadingStatus.COMPLETED)
                .build();
        readingListService.addToShelf(BOOK_ID, request);

        ArgumentCaptor<ReadingList> captor = ArgumentCaptor.forClass(ReadingList.class);
        verify(readingListRepository).save(captor.capture());
        assertThat(captor.getValue().getCompletedAt()).isNotNull();
        assertThat(challenge.getCompletedBooks()).isEqualTo(1);
    }

    @Test
    void addToShelf_statusReading_setsStartedAt() {
        when(bookRepository.existsById(BOOK_ID)).thenReturn(true);
        when(readingListRepository.existsByUserIdAndBookId(USER_A, BOOK_ID)).thenReturn(false);
        when(readingListMapper.toReadingListResponse(any()))
                .thenReturn(ReadingListResponse.builder().build());

        ReadingListCreateRequest request = ReadingListCreateRequest.builder()
                .bookId(BOOK_ID)
                .status(ReadingStatus.READING)
                .build();
        readingListService.addToShelf(BOOK_ID, request);

        ArgumentCaptor<ReadingList> captor = ArgumentCaptor.forClass(ReadingList.class);
        verify(readingListRepository).save(captor.capture());
        assertThat(captor.getValue().getStartedAt()).isNotNull();
        assertThat(captor.getValue().getCompletedAt()).isNull();
    }

    @Test
    void addToShelf_statusWantToRead_leavesTimestampsNull() {
        when(bookRepository.existsById(BOOK_ID)).thenReturn(true);
        when(readingListRepository.existsByUserIdAndBookId(USER_A, BOOK_ID)).thenReturn(false);
        when(readingListMapper.toReadingListResponse(any()))
                .thenReturn(ReadingListResponse.builder().build());

        ReadingListCreateRequest request =
                ReadingListCreateRequest.builder().bookId(BOOK_ID).build();
        readingListService.addToShelf(BOOK_ID, request);

        ArgumentCaptor<ReadingList> captor = ArgumentCaptor.forClass(ReadingList.class);
        verify(readingListRepository).save(captor.capture());
        assertThat(captor.getValue().getStartedAt()).isNull();
        assertThat(captor.getValue().getCompletedAt()).isNull();
    }
}
