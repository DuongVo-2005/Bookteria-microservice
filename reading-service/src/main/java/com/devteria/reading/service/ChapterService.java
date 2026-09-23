package com.devteria.reading.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.devteria.reading.dto.request.ChapterCreateRequest;
import com.devteria.reading.dto.response.ChapterResponse;
import com.devteria.reading.dto.response.ChapterSummaryResponse;
import com.devteria.reading.entity.Chapter;
import com.devteria.reading.exception.AppException;
import com.devteria.reading.exception.ErrorCode;
import com.devteria.reading.mapper.ChapterMapper;
import com.devteria.reading.repository.ChapterRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChapterService {
    ChapterRepository chapterRepository;
    ChapterMapper chapterMapper;

    public Page<ChapterSummaryResponse> getChapters(String bookId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chapterRepository
                .findByBookIdOrderByChapterNumberAsc(bookId, pageable)
                .map(chapterMapper::toChapterSummaryResponse);
    }

    public ChapterResponse getChapterContent(String bookId, int chapterNumber) {
        Chapter chapter = chapterRepository
                .findByBookIdAndChapterNumber(bookId, chapterNumber)
                .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));
        return chapterMapper.toChapterResponse(chapter);
    }

    public ChapterResponse createChapter(String bookId, ChapterCreateRequest request) {
        Chapter chapter = Chapter.builder()
                .bookId(bookId)
                .chapterNumber(request.getChapterNumber())
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .content(request.getContent())
                .readTimeMinutes(request.getReadTimeMinutes())
                .build();

        chapter = chapterRepository.save(chapter);
        return chapterMapper.toChapterResponse(chapter);
    }
}
