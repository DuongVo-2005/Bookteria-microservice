package com.devteria.book.mapper;

import org.mapstruct.Mapper;

import com.devteria.book.dto.response.ReviewResponse;
import com.devteria.book.entity.Review;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    ReviewResponse toReviewResponse(Review review);
}
