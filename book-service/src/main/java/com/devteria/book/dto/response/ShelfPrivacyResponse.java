package com.devteria.book.dto.response;

import com.devteria.book.dto.ShelfPrivacy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShelfPrivacyResponse {
    ShelfPrivacy privacy;
}
