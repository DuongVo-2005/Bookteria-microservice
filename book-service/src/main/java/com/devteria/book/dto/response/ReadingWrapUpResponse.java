package com.devteria.book.dto.response;

import java.time.Instant;
import java.util.List;

import com.devteria.book.dto.WrapUpPeriod;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA v2 §4.2: Reading Wrap-up - dữ liệu để FE tự vẽ "Stats Card" (tương tự Quote Card ở
// FEAT-04, canvas rendering là việc của FE, BE chỉ cấp dữ liệu thật).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingWrapUpResponse {
    WrapUpPeriod period;
    Instant from;
    Instant to;
    long booksCompleted;
    List<String> topCategories;
    List<String> topAuthors;
}
