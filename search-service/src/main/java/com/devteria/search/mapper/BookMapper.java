package com.devteria.search.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import com.devteria.search.dto.response.BookSearchResponse;
import com.devteria.search.dto.response.PageResponse;
import com.devteria.search.entity.BookDocument;

@Mapper(componentModel = "spring")
public interface BookMapper {
    @Mapping(source = "number", target = "currentPage")
    @Mapping(source = "size", target = "pageSize")
    @Mapping(source = "content", target = "data")
    PageResponse<BookSearchResponse> toPageResponse(Page<BookDocument> bookDocumentPage);

    BookSearchResponse toBookSearchResponse(BookDocument document);
}
