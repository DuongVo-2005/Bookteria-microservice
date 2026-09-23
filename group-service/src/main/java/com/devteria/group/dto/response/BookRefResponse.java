package com.devteria.group.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookRefResponse {
    String bookId;
    String bookTitle;
    String authorName;
    String coverImage;
    boolean deleted; // xem ghi chú ở entity/BookRef.java về vì sao không đặt tên "isDeleted"
}
