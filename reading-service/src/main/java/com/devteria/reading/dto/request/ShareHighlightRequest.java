package com.devteria.reading.dto.request;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-04: body optional cho POST /highlights/{id}/share - null/rỗng vẫn share dạng
// text-only như hành vi cũ (Phase 5 §18.1), không phải trường bắt buộc.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShareHighlightRequest {
    List<String> mediaUrls;
}
