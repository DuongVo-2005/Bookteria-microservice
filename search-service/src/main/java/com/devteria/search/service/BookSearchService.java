package com.devteria.search.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.devteria.search.dto.response.BookSearchResponse;
import com.devteria.search.dto.response.PageResponse;
import com.devteria.search.entity.BookDocument;
import com.devteria.search.mapper.BookMapper;
import com.devteria.search.repository.BookSearchRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookSearchService {
    BookSearchRepository bookSearchRepository;

    BookMapper bookMapper;

    public PageResponse<BookSearchResponse> search(
            String q, String categoryId, String authorId, String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        boolean hasQ = q != null && !q.isBlank();
        boolean hasCategory = categoryId != null && !categoryId.isBlank();
        boolean hasAuthor = authorId != null && !authorId.isBlank();

        Page<BookDocument> bookPage;
        if (hasQ) {
            bookPage = bookSearchRepository.searchByKeyword(q, pageable);
        } else if (hasCategory && hasAuthor) {
            bookPage = bookSearchRepository.findByCategoryIdsContainingAndAuthorIdsContaining(
                    categoryId, authorId, pageable);
        } else if (hasCategory) {
            bookPage = bookSearchRepository.findByCategoryIdsContaining(categoryId, pageable);
        } else if (hasAuthor) {
            bookPage = bookSearchRepository.findByAuthorIdsContaining(authorId, pageable);
        } else {
            bookPage = bookSearchRepository.findAll(pageable);
        }
        return bookMapper.toPageResponse(bookPage);
    }

    // Trả về danh sách TỪ KHOÁ gợi ý (tiêu đề sách khớp tiền tố), không phải kết quả sách đầy đủ —
    // đúng kiểu ô tìm kiếm gợi ý-khi-gõ (FE dropdown gợi ý), khác hẳn search() ở trên.
    public List<String> suggest(String q, int limit) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        int boundedLimit = Math.max(1, Math.min(limit, 20));
        Pageable pageable = PageRequest.of(0, boundedLimit);

        return bookSearchRepository.suggestByTitlePrefix(q, pageable).stream()
                .map(BookDocument::getTitle)
                .distinct()
                .toList();
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }
        return switch (sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "rating" ->
                Sort.by(Sort.Direction.DESC, "ratingAverage").and(Sort.by(Sort.Direction.DESC, "ratingCount"));
            default -> Sort.unsorted();
        };
    }
}
