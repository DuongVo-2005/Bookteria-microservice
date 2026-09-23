package com.devteria.reading.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.devteria.reading.dto.ReadingProgressStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA v2 §3.1 Offline Sync Batch: 1 item tiến độ đọc trong hàng đợi offline của client -
// phẳng (không lồng ReadingProgressUpdateRequest) để FE serialize đơn giản, có thêm bookId (path
// variable ở endpoint /progress/{bookId} online) và clientUpdatedAt (bắt buộc, khác endpoint
// online vì đây chính là input cho monotonicity check ở ReadingProgressService).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProgressSyncItem {
    @NotBlank(message = "FIELD_REQUIRED")
    String bookId;

    @NotBlank(message = "FIELD_REQUIRED")
    String currentChapterId;

    int currentChapterIndex;

    int progressPercent;

    @NotNull
    ReadingProgressStatus status;

    @NotNull
    Instant clientUpdatedAt;
}
