package com.devteria.reading.mapper;

import org.mapstruct.Mapper;

import com.devteria.reading.dto.response.ChapterResponse;
import com.devteria.reading.dto.response.ChapterSummaryResponse;
import com.devteria.reading.entity.Chapter;

@Mapper(componentModel = "spring")
public interface ChapterMapper {
    ChapterResponse toChapterResponse(Chapter chapter);

    ChapterSummaryResponse toChapterSummaryResponse(Chapter chapter);
}
