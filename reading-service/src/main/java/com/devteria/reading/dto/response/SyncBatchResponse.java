package com.devteria.reading.dto.response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyncBatchResponse {
    int progressApplied;

    // idea-spec BA v2 §3.1: số item tiến độ bị bỏ qua vì clientUpdatedAt cũ hơn progress đã lưu
    // (Progress Monotonicity) - FE nên hiển thị/log để người dùng biết 1 phần batch không được
    // áp dụng, không phải lỗi.
    int progressSkipped;

    List<HighlightResponse> highlightsCreated;
    List<BookmarkResponse> bookmarksCreated;
}
