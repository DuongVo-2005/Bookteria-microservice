package com.devteria.book.mapper;

import org.mapstruct.Mapper;

import com.devteria.book.dto.response.ReadingListResponse;
import com.devteria.book.entity.ReadingList;

@Mapper(componentModel = "spring")
public interface ReadingListMapper {
    ReadingListResponse toReadingListResponse(ReadingList readingList);
}
