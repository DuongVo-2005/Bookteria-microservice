package com.devteria.reading.entity;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-02: Daily Reading Streak. Dùng mỗi lần gọi ReadingProgressService#updateProgress()
// làm tín hiệu "có đọc hôm nay" — hệ thống hiện KHÔNG có bảng ReadingSession/durationSeconds nào
// (tính năng "Lịch sử Phiên đọc" ở §3.1 idea-spec chưa được xây, không nằm trong Phase 2 backlog
// này) nên KHÔNG enforce được điều kiện "durationSeconds >= 5 phút" ghi trong văn bản BA gốc —
// chấp nhận xấp xỉ này, ghi lại là quyết định phạm vi rõ ràng trong be-report.md, không âm thầm bỏ qua.
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reading_streaks")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingStreak {
    @MongoId
    String userId;

    @Builder.Default
    int currentStreak = 0;

    @Builder.Default
    int longestStreak = 0;

    LocalDate lastActiveDate;

    @Builder.Default
    List<String> badges = new java.util.ArrayList<>();
}
