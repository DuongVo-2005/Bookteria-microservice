package com.devteria.reading.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.devteria.reading.dto.request.ProgressSyncItem;
import com.devteria.reading.dto.request.ReadingProgressUpdateRequest;
import com.devteria.reading.dto.request.SyncBatchRequest;
import com.devteria.reading.dto.response.BookmarkResponse;
import com.devteria.reading.dto.response.HighlightResponse;
import com.devteria.reading.dto.response.SyncBatchResponse;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec BA v2 §3.1 Offline Sync Batch: điểm vào duy nhất áp dụng cả 1 lô write tích luỹ từ
// hàng đợi offline của client (FE tự giữ hàng đợi, service này không biết gì về việc client từng
// offline). Tái dùng nguyên service/logic single-item đã có (ReadingProgressService,
// HighlightService, BookmarkService) thay vì viết lại - chỉ khác ở chỗ mỗi item mang theo
// timestamp thật từ lúc tạo ở client thay vì Instant.now() lúc server nhận request.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SyncBatchService {
    ReadingProgressService readingProgressService;
    HighlightService highlightService;
    BookmarkService bookmarkService;

    public SyncBatchResponse applyBatch(SyncBatchRequest request) {
        int applied = 0;
        int skipped = 0;

        if (request.getProgress() != null) {
            for (ProgressSyncItem item : request.getProgress()) {
                ReadingProgressUpdateRequest progressRequest = ReadingProgressUpdateRequest.builder()
                        .currentChapterId(item.getCurrentChapterId())
                        .currentChapterIndex(item.getCurrentChapterIndex())
                        .progressPercent(item.getProgressPercent())
                        .status(item.getStatus())
                        .clientUpdatedAt(item.getClientUpdatedAt())
                        .build();
                boolean wasApplied = readingProgressService
                        .applyProgressUpdate(item.getBookId(), progressRequest)
                        .applied();
                if (wasApplied) {
                    applied++;
                } else {
                    skipped++;
                }
            }
        }

        List<HighlightResponse> highlightsCreated = request.getHighlights() == null
                ? List.of()
                : request.getHighlights().stream()
                        .map(highlightService::createHighlight)
                        .toList();

        List<BookmarkResponse> bookmarksCreated = request.getBookmarks() == null
                ? List.of()
                : request.getBookmarks().stream()
                        .map(bookmarkService::createBookmark)
                        .toList();

        return SyncBatchResponse.builder()
                .progressApplied(applied)
                .progressSkipped(skipped)
                .highlightsCreated(highlightsCreated)
                .bookmarksCreated(bookmarksCreated)
                .build();
    }
}
