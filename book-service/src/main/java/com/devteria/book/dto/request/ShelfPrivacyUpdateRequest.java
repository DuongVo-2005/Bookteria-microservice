package com.devteria.book.dto.request;

import jakarta.validation.constraints.NotNull;

import com.devteria.book.dto.ShelfPrivacy;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShelfPrivacyUpdateRequest {
    @NotNull(message = "SHELF_PRIVACY_REQUIRED")
    ShelfPrivacy privacy;
}
