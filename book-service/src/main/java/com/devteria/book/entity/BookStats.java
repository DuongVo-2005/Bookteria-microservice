package com.devteria.book.entity;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookStats {
    BigDecimal ratingAverage;
    Long reviewCount;
    Long ratingCount;
    Long readCount;
    Long wantToReadCount;
}
