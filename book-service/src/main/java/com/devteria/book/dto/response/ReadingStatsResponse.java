package com.devteria.book.dto.response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingStatsResponse {
    long booksWantToRead;
    long booksReading;
    long booksCompleted;
    long totalBooks;

    // 12 tháng gần nhất, kể cả tháng có count=0 (FE vẽ biểu đồ không cần tự lấp khoảng trống).
    List<MonthlyReadingCount> completedByMonth;
}
