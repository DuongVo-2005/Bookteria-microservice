package com.devteria.reading.mapper;

import org.mapstruct.Mapper;

import com.devteria.reading.dto.response.ReadingProgressResponse;
import com.devteria.reading.entity.ReadingProgress;

@Mapper(componentModel = "spring")
public interface ReadingProgressMapper {
    ReadingProgressResponse toReadingProgressResponse(ReadingProgress readingProgress);
}
