package com.devteria.search.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import com.devteria.search.dto.PostIndexPayload;
import com.devteria.search.dto.response.PageResponse;
import com.devteria.search.dto.response.PostSearchResponse;
import com.devteria.search.entity.PostDocument;

@Mapper(componentModel = "spring")
public interface PostDocumentMapper {
    @Mapping(source = "postId", target = "id")
    PostDocument toDocument(PostIndexPayload payload);

    PostSearchResponse toPostSearchResponse(PostDocument document);

    @Mapping(source = "number", target = "currentPage")
    @Mapping(source = "size", target = "pageSize")
    @Mapping(source = "content", target = "data")
    PageResponse<PostSearchResponse> toPageResponse(Page<PostDocument> postDocumentPage);
}
