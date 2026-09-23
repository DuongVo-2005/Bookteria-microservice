package com.devteria.reading.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingStreakResponse {
    int currentStreak;
    int longestStreak;
    LocalDate lastActiveDate;
    List<String> badges;
}
