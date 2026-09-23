package com.devteria.reading.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.reading.dto.request.HighlightCreateRequest;
import com.devteria.reading.dto.request.HighlightUpdateRequest;
import com.devteria.reading.dto.request.PostCreateRequest;
import com.devteria.reading.dto.request.ShareHighlightRequest;
import com.devteria.reading.dto.response.BookLookupResponse;
import com.devteria.reading.dto.response.HighlightResponse;
import com.devteria.reading.dto.response.PostSummaryResponse;
import com.devteria.reading.dto.response.QuoteCardResponse;
import com.devteria.reading.entity.Highlight;
import com.devteria.reading.exception.AppException;
import com.devteria.reading.exception.ErrorCode;
import com.devteria.reading.mapper.HighlightMapper;
import com.devteria.reading.repository.HighlightRepository;
import com.devteria.reading.repository.httpclient.BookClient;
import com.devteria.reading.repository.httpclient.PostClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class HighlightService {
    private static final Set<String> VALID_COLORS = Set.of("yellow", "green", "pink", "blue", "purple");

    HighlightRepository highlightRepository;
    HighlightMapper highlightMapper;
    PostClient postClient;
    BookClient bookClient;

    public List<HighlightResponse> getHighlights(String bookId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        return highlightRepository.findByUserIdAndBookId(userId, bookId).stream()
                .map(highlightMapper::toHighlightResponse)
                .toList();
    }

    public HighlightResponse createHighlight(HighlightCreateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        // idea-spec BA v2 §3.1: retry của cùng 1 batch (vd network timeout dù server đã xử lý
        // xong) trả lại đúng bản ghi đã tạo lần đầu thay vì tạo trùng - chỉ áp dụng khi client có
        // gửi clientId, không đổi hành vi tạo online trực tiếp (clientId luôn null).
        if (request.getClientId() != null) {
            var existing = highlightRepository.findByUserIdAndClientId(userId, request.getClientId());
            if (existing.isPresent()) {
                return highlightMapper.toHighlightResponse(existing.get());
            }
        }

        String color = normalizeColor(request.getColor());

        Highlight highlight = Highlight.builder()
                .userId(userId)
                .bookId(request.getBookId())
                .chapterId(request.getChapterId())
                .chapterNumber(request.getChapterNumber())
                .chapterTitle(request.getChapterTitle())
                .selectedText(request.getSelectedText())
                .color(color)
                .note(request.getNote())
                .createdAt(request.getCreatedAt() != null ? request.getCreatedAt() : Instant.now())
                .clientId(request.getClientId())
                .build();

        highlight = highlightRepository.save(highlight);
        return highlightMapper.toHighlightResponse(highlight);
    }

    public HighlightResponse updateHighlight(String highlightId, HighlightUpdateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Highlight highlight = highlightRepository
                .findByIdAndUserId(highlightId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.HIGHLIGHT_NOT_FOUND));

        highlight.setNote(request.getNote());

        highlight = highlightRepository.save(highlight);
        return highlightMapper.toHighlightResponse(highlight);
    }

    public void deleteHighlight(String highlightId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Highlight highlight = highlightRepository
                .findByIdAndUserId(highlightId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.HIGHLIGHT_NOT_FOUND));

        highlightRepository.delete(highlight);
    }

    // idea-spec Phase 5 - "18.1 Book Quotes/Highlights Sharing": chuyển Highlight/Note thành
    // Post trên Global Feed của post-service. post-service tự enrich bookTitle/bookCoverImage
    // qua bookId có sẵn trên Post - reading-service chỉ cần build content + gửi kèm bookId.
    // idea-spec BA FEAT-04: request giờ nhận thêm mediaUrls (optional) - ảnh Quote Card FE tự
    // render + upload qua file-service trước khi gọi endpoint này; không truyền vẫn share
    // text-only như hành vi cũ.
    public PostSummaryResponse shareToPost(String highlightId, ShareHighlightRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Highlight highlight = highlightRepository
                .findByIdAndUserId(highlightId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.HIGHLIGHT_NOT_FOUND));

        StringBuilder content = new StringBuilder();
        content.append("\"").append(highlight.getSelectedText()).append("\"");
        if (highlight.getNote() != null && !highlight.getNote().isBlank()) {
            content.append("\n\n").append(highlight.getNote());
        }

        try {
            var response = postClient.createPost(PostCreateRequest.builder()
                    .content(content.toString())
                    .bookId(highlight.getBookId())
                    .mediaUrls(request != null ? request.getMediaUrls() : null)
                    .build());
            if (response == null || response.getResult() == null) {
                throw new AppException(ErrorCode.SHARE_TO_POST_FAILED);
            }
            return response.getResult();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not share highlight {} as a post", highlightId, e);
            throw new AppException(ErrorCode.SHARE_TO_POST_FAILED);
        }
    }

    // idea-spec BA FEAT-04: Quote Card Generator - trả nội dung + tên sách/tác giả thật để FE tự
    // vẽ thiệp (chọn font/size/màu nền/ảnh nền là việc của FE, canvas rendering không thuộc BE).
    public QuoteCardResponse getQuoteCardData(String highlightId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Highlight highlight = highlightRepository
                .findByIdAndUserId(highlightId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.HIGHLIGHT_NOT_FOUND));

        var bookResponse = bookClient.getBookById(highlight.getBookId());
        if (bookResponse == null || bookResponse.getResult() == null) {
            throw new AppException(ErrorCode.BOOK_NOT_FOUND);
        }
        BookLookupResponse book = bookResponse.getResult();

        String authorName = book.getAuthors() != null && !book.getAuthors().isEmpty()
                ? book.getAuthors().getFirst().getName()
                : null;

        return QuoteCardResponse.builder()
                .text(highlight.getSelectedText())
                .note(highlight.getNote())
                .bookTitle(book.getTitle())
                .authorName(authorName)
                .bookCoverImage(book.getMetadata() != null ? book.getMetadata().getCoverImage() : null)
                .build();
    }

    private String normalizeColor(String color) {
        if (color == null) {
            throw new AppException(ErrorCode.INVALID_HIGHLIGHT_COLOR);
        }
        String normalized = color.toLowerCase();
        if (!VALID_COLORS.contains(normalized)) {
            throw new AppException(ErrorCode.INVALID_HIGHLIGHT_COLOR);
        }
        return normalized;
    }
}
