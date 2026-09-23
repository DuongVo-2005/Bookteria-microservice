package com.devteria.post.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Bản rút gọn của book-service's ReadingListResponse - chỉ cần bookId cho Personalized Feed
// (idea-spec Phase 6 - "22. Personalized Feed"). @JsonIgnoreProperties bắt buộc vì response thật
// có nhiều field khác (status, currentPage, progressPercent, ...) - thiếu annotation này sẽ ném
// UnrecognizedPropertyException (gotcha đã lặp lại nhiều lần trong project, xem be-report.md).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReadingListItemResponse {
    String bookId;
}
