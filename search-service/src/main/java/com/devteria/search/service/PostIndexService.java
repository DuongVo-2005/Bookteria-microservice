package com.devteria.search.service;

import org.springframework.stereotype.Service;

import com.devteria.search.client.PostServiceClient;
import com.devteria.search.dto.PostFeedItemResponse;
import com.devteria.search.dto.PostIndexPayload;
import com.devteria.search.dto.response.ApiResponse;
import com.devteria.search.dto.response.PageResponse;
import com.devteria.search.mapper.PostDocumentMapper;
import com.devteria.search.repository.PostSearchRepository;

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
public class PostIndexService {
    PostSearchRepository postSearchRepository;
    PostDocumentMapper postDocumentMapper;
    PostServiceClient postServiceClient;

    // idea-spec Phase 6 - "22.1 Search Integration": chỉ index post PUBLIC - post FRIENDS/PRIVATE
    // không bao giờ vào index tìm kiếm chung, tôn trọng đúng visibility gốc.
    public void upsert(PostIndexPayload payload) {
        if (!"PUBLIC".equals(payload.getVisibility())) {
            return;
        }
        postSearchRepository.save(postDocumentMapper.toDocument(payload));
    }

    public void delete(String postId) {
        postSearchRepository.deleteById(postId);
    }

    // idea-spec Phase 3 - "5. Global Feed" (GET /post/feed) đã lọc sẵn PUBLIC - tái dùng đúng
    // endpoint đó cho reindex, không cần endpoint mới ở post-service. Response shape
    // (PostFeedItemResponse: id/userId) khác payload Kafka (PostIndexPayload: postId/authorId) -
    // tự map qua lại ở đây.
    public int reindexAll() {
        final int size = 50;
        int totalIndexed = 0;
        ApiResponse<PageResponse<PostFeedItemResponse>> response = postServiceClient.getGlobalFeed(0, size);
        if (response == null || response.getResult() == null) {
            return 0;
        }
        PageResponse<PostFeedItemResponse> firstPage = response.getResult();

        int totalPage = firstPage.getTotalPages();
        for (int page = 0; page < totalPage; page++) {
            PageResponse<PostFeedItemResponse> currentPage;
            if (page == 0) {
                currentPage = firstPage;
            } else {
                ApiResponse<PageResponse<PostFeedItemResponse>> pageResponse =
                        postServiceClient.getGlobalFeed(page, size);
                currentPage = pageResponse.getResult();
            }
            if (currentPage.getData() == null) {
                continue;
            }
            for (PostFeedItemResponse item : currentPage.getData()) {
                upsert(toIndexPayload(item));
                totalIndexed++;
            }
        }

        return totalIndexed;
    }

    private PostIndexPayload toIndexPayload(PostFeedItemResponse item) {
        return new PostIndexPayload(
                item.getId(),
                item.getContent(),
                item.getUserId(),
                item.getHashtags(),
                item.getBookId(),
                item.getVisibility(),
                item.getCreatedDate());
    }
}
