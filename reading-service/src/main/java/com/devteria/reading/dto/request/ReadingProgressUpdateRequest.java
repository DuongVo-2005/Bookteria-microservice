package com.devteria.reading.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.devteria.reading.dto.ReadingProgressStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingProgressUpdateRequest {
    @NotBlank
    String currentChapterId;

    @Min(0)
    int currentChapterIndex;

    @Min(0)
    @Max(100)
    int progressPercent;

    @NotNull
    ReadingProgressStatus status;

    // idea-spec BA v2 §3.1 Offline Sync Batch: thời điểm client (offline) thực sự tạo ra write
    // này - null khi gọi trực tiếp online như cũ (khi đó luôn coi là "vừa xảy ra"). Dùng để chặn
    // 1 write cũ hơn ghi đè lên progress mới hơn đã có khi áp dụng cả 1 batch không theo thứ tự.
    Instant clientUpdatedAt;
}
