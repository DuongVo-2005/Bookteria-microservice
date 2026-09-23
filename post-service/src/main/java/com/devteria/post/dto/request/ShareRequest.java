package com.devteria.post.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShareRequest {
    // Optional — cho phép share không kèm lời bình.
    String content;
}
