package com.devteria.reading.dto.request;

import java.util.List;

import jakarta.validation.Valid;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA v2 §3.1 Offline Sync Batch: áp dụng cả 1 lô ghi (tiến độ đọc + highlight +
// bookmark) tích luỹ khi client offline, trong 1 lần gọi khi có mạng trở lại. Mỗi danh sách
// optional (null = không có item loại đó trong batch này).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyncBatchRequest {
    @Valid
    List<ProgressSyncItem> progress;

    @Valid
    List<HighlightCreateRequest> highlights;

    @Valid
    List<BookmarkCreateRequest> bookmarks;
}
