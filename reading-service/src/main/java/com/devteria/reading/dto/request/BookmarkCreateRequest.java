package com.devteria.reading.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookmarkCreateRequest {
    @NotBlank
    String bookId;

    @NotBlank
    String chapterId;

    int chapterNumber;

    String chapterTitle;

    int positionPercent;

    String snippet;

    String note;

    // idea-spec BA v2 §3.1 Offline Sync Batch: thời điểm tạo thật ở client offline - null khi
    // gọi trực tiếp online như cũ (giữ nguyên hành vi, dùng Instant.now() trong service).
    Instant createdAt;

    // idea-spec BA v2 §3.1: idempotency key client tự sinh - null khi tạo online trực tiếp
    // (không dedupe, giữ nguyên hành vi cũ). Xem BookmarkService.createBookmark().
    String clientId;
}
