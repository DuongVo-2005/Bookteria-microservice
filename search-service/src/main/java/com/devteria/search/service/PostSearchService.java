package com.devteria.search.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.devteria.search.dto.response.PageResponse;
import com.devteria.search.dto.response.PostSearchResponse;
import com.devteria.search.entity.PostDocument;
import com.devteria.search.mapper.PostDocumentMapper;
import com.devteria.search.repository.PostSearchRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostSearchService {
    private static final int TRENDING_HASHTAG_SAMPLE_SIZE = 500;

    PostSearchRepository postSearchRepository;
    PostDocumentMapper postDocumentMapper;

    public PageResponse<PostSearchResponse> search(String q, String hashtag, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        boolean hasQ = q != null && !q.isBlank();
        boolean hasHashtag = hashtag != null && !hashtag.isBlank();

        Page<PostDocument> postPage;
        if (hasQ) {
            postPage = postSearchRepository.searchByKeyword(q, pageable);
        } else if (hasHashtag) {
            postPage = postSearchRepository.findByHashtagsContaining(hashtag, pageable);
        } else {
            postPage = postSearchRepository.findAll(pageable);
        }
        return postDocumentMapper.toPageResponse(postPage);
    }

    // idea-spec Phase 6 - "22.1 Search Integration" (Hashtag). Không dùng Elasticsearch terms
    // aggregation - đếm trong Java trên mẫu N post gần nhất (cùng lý do nhất quán với các quyết
    // định "group trong Java" khác trong Phase 5/6: đơn giản, đủ dùng ở quy mô hiện tại, không
    // cần học/verify cú pháp Aggregation API mới của Elasticsearch Java client cho 1 tính năng
    // phụ). Không phải Hashtag KHÔNG có index riêng - nó vốn chỉ là 1 field trong PostDocument,
    // tạo hẳn 1 index riêng chỉ để lưu lại đúng dữ liệu đã có là dư thừa.
    public List<String> trendingHashtags(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        Pageable pageable =
                PageRequest.of(0, TRENDING_HASHTAG_SAMPLE_SIZE, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<PostDocument> recentPosts = postSearchRepository.findAll(pageable);

        Map<String, Long> frequency = new HashMap<>();
        for (PostDocument post : recentPosts) {
            if (post.getHashtags() == null) {
                continue;
            }
            for (String tag : post.getHashtags()) {
                frequency.merge(tag, 1L, Long::sum);
            }
        }

        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(boundedLimit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
