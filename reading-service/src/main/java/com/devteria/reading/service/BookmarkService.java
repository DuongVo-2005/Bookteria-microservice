package com.devteria.reading.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.reading.dto.request.BookmarkCreateRequest;
import com.devteria.reading.dto.response.BookmarkResponse;
import com.devteria.reading.entity.Bookmark;
import com.devteria.reading.exception.AppException;
import com.devteria.reading.exception.ErrorCode;
import com.devteria.reading.mapper.BookmarkMapper;
import com.devteria.reading.repository.BookmarkRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookmarkService {
    BookmarkRepository bookmarkRepository;
    BookmarkMapper bookmarkMapper;

    public List<BookmarkResponse> getBookmarks(String bookId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        return bookmarkRepository.findByUserIdAndBookId(userId, bookId).stream()
                .map(bookmarkMapper::toBookmarkResponse)
                .toList();
    }

    public BookmarkResponse createBookmark(BookmarkCreateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        // idea-spec BA v2 §3.1: retry của cùng 1 batch trả lại đúng bản ghi đã tạo lần đầu thay
        // vì tạo trùng - chỉ áp dụng khi client có gửi clientId (giữ nguyên hành vi tạo online).
        if (request.getClientId() != null) {
            var existing = bookmarkRepository.findByUserIdAndClientId(userId, request.getClientId());
            if (existing.isPresent()) {
                return bookmarkMapper.toBookmarkResponse(existing.get());
            }
        }

        Bookmark bookmark = Bookmark.builder()
                .userId(userId)
                .bookId(request.getBookId())
                .chapterId(request.getChapterId())
                .chapterNumber(request.getChapterNumber())
                .chapterTitle(request.getChapterTitle())
                .positionPercent(request.getPositionPercent())
                .clientId(request.getClientId())
                .snippet(request.getSnippet())
                .note(request.getNote())
                .createdAt(request.getCreatedAt() != null ? request.getCreatedAt() : Instant.now())
                .build();

        bookmark = bookmarkRepository.save(bookmark);
        return bookmarkMapper.toBookmarkResponse(bookmark);
    }

    public void deleteBookmark(String bookmarkId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Bookmark bookmark = bookmarkRepository
                .findByIdAndUserId(bookmarkId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKMARK_NOT_FOUND));

        bookmarkRepository.delete(bookmark);
    }
}
