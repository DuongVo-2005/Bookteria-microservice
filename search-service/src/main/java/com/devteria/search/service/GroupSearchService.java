package com.devteria.search.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.devteria.search.dto.response.GroupSearchResponse;
import com.devteria.search.dto.response.PageResponse;
import com.devteria.search.entity.GroupDocument;
import com.devteria.search.mapper.GroupDocumentMapper;
import com.devteria.search.repository.GroupSearchRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec Phase 6 - "22.1 Search Integration" ("Group nổi bật"): search theo từ khoá tên/mô
// tả/category - xếp hạng "nổi bật" (memberCount/hoạt động) đã có sẵn ở group-service's
// GET /groups/popular (Phase 6 §20), không duplicate lại ở đây, xem entity/GroupDocument.java.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupSearchService {
    GroupSearchRepository groupSearchRepository;
    GroupDocumentMapper groupDocumentMapper;

    public PageResponse<GroupSearchResponse> search(String q, String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasQ = q != null && !q.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        Page<GroupDocument> groupPage;
        if (hasQ) {
            groupPage = groupSearchRepository.searchByKeyword(q, pageable);
        } else if (hasCategory) {
            groupPage = groupSearchRepository.findByCategory(category, pageable);
        } else {
            groupPage = groupSearchRepository.findAll(pageable);
        }
        return groupDocumentMapper.toPageResponse(groupPage);
    }
}
