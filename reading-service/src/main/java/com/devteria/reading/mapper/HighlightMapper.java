package com.devteria.reading.mapper;

import org.mapstruct.Mapper;

import com.devteria.reading.dto.response.HighlightResponse;
import com.devteria.reading.entity.Highlight;

@Mapper(componentModel = "spring")
public interface HighlightMapper {
    HighlightResponse toHighlightResponse(Highlight highlight);
}
