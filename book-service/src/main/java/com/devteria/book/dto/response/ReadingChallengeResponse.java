package com.devteria.book.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingChallengeResponse {
    int year;
    int targetBooks;
    int completedBooks;
    double percent;
}
