package com.devteria.search.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import com.devteria.search.dto.GroupIndexPayload;
import com.devteria.search.dto.response.GroupSearchResponse;
import com.devteria.search.dto.response.PageResponse;
import com.devteria.search.entity.GroupDocument;

@Mapper(componentModel = "spring")
public interface GroupDocumentMapper {
    @Mapping(source = "groupId", target = "id")
    GroupDocument toDocument(GroupIndexPayload payload);

    GroupSearchResponse toGroupSearchResponse(GroupDocument document);

    @Mapping(source = "number", target = "currentPage")
    @Mapping(source = "size", target = "pageSize")
    @Mapping(source = "content", target = "data")
    PageResponse<GroupSearchResponse> toPageResponse(Page<GroupDocument> groupDocumentPage);
}
