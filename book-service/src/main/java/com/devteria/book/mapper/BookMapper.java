package com.devteria.book.mapper;

import org.mapstruct.Mapper;

import com.devteria.book.dto.response.BookResponse;
import com.devteria.book.entity.Book;

@Mapper(componentModel = "spring")
public interface BookMapper {
    BookResponse toBookResponse(Book book);
}
