package com.devteria.search.service;

import org.springframework.stereotype.Service;

import com.devteria.search.client.GroupServiceClient;
import com.devteria.search.dto.GroupIndexPayload;
import com.devteria.search.dto.GroupListItemResponse;
import com.devteria.search.dto.response.ApiResponse;
import com.devteria.search.dto.response.PageResponse;
import com.devteria.search.mapper.GroupDocumentMapper;
import com.devteria.search.repository.GroupSearchRepository;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Builder
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupIndexService {
    GroupSearchRepository groupSearchRepository;
    GroupDocumentMapper groupDocumentMapper;
    GroupServiceClient groupServiceClient;

    // idea-spec Phase 6 - "22.1 Search Integration": chỉ index group PUBLIC - group PRIVATE
    // không bao giờ vào index tìm kiếm chung, tôn trọng đúng visibility gốc.
    public void upsert(GroupIndexPayload payload) {
        if (!"PUBLIC".equals(payload.getVisibility())) {
            return;
        }
        groupSearchRepository.save(groupDocumentMapper.toDocument(payload));
    }

    public void delete(String groupId) {
        groupSearchRepository.deleteById(groupId);
    }

    // GET /groups (listGroups) không lọc theo visibility ở group-service - tự lọc PUBLIC ở đây
    // (defense in depth, đúng nguyên tắc "index tìm kiếm chung không bao giờ chứa nội dung riêng
    // tư" dù nguồn dữ liệu có lọc đúng hay không).
    public int reindexAll() {
        final int size = 50;
        int totalIndexed = 0;
        ApiResponse<PageResponse<GroupListItemResponse>> response = groupServiceClient.listGroups(0, size);
        if (response == null || response.getResult() == null) {
            return 0;
        }
        PageResponse<GroupListItemResponse> firstPage = response.getResult();

        int totalPage = firstPage.getTotalPages();
        for (int page = 0; page < totalPage; page++) {
            PageResponse<GroupListItemResponse> currentPage;
            if (page == 0) {
                currentPage = firstPage;
            } else {
                ApiResponse<PageResponse<GroupListItemResponse>> pageResponse =
                        groupServiceClient.listGroups(page, size);
                currentPage = pageResponse.getResult();
            }
            if (currentPage.getData() == null) {
                continue;
            }
            for (GroupListItemResponse item : currentPage.getData()) {
                upsert(toIndexPayload(item));
                totalIndexed++;
            }
        }

        return totalIndexed;
    }

    private GroupIndexPayload toIndexPayload(GroupListItemResponse item) {
        return new GroupIndexPayload(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getCategory(),
                item.getVisibility(),
                item.getCreatedAt());
    }
}
