package com.devteria.reading.mapper;

import org.mapstruct.Mapper;

import com.devteria.reading.dto.response.BookmarkResponse;
import com.devteria.reading.entity.Bookmark;

@Mapper(componentModel = "spring")
public interface BookmarkMapper {
    BookmarkResponse toBookmarkResponse(Bookmark bookmark);
}
