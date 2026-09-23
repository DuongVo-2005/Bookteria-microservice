package com.devteria.search.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.devteria.search.client.BookServiceClient;
import com.devteria.search.dto.BookIndexPayload;
import com.devteria.search.dto.response.ApiResponse;
import com.devteria.search.dto.response.PageResponse;
import com.devteria.search.mapper.BookDocumentMapper;
import com.devteria.search.repository.BookSearchRepository;

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
public class BookIndexService {
    BookSearchRepository bookSearchRepository;
    BookDocumentMapper bookDocumentMapper;
    BookServiceClient bookServiceClient;

    public void upsert(BookIndexPayload payload) {
        bookSearchRepository.save(bookDocumentMapper.toDocument(payload));
    }

    public void delete(String bookId) {
        bookSearchRepository.deleteById(bookId);
    }

    public void updateRating(String bookId, BigDecimal ratingAverage, Long ratingCount) {
        bookSearchRepository
                .findById(bookId)
                .ifPresentOrElse(
                        bookDocument -> {
                            bookDocument.setRatingAverage(ratingAverage);
                            bookDocument.setRatingCount(ratingCount);
                            bookSearchRepository.save(bookDocument);
                        },
                        () -> log.warn(
                                "Book document not found when updating rating. bookId={}, ratingAverage={}, ratingCount={}",
                                bookId,
                                ratingAverage,
                                ratingCount));
    }

    public int reindexAll() {
        final int size = 50;
        int totalIndexed = 0;
        ApiResponse<PageResponse<BookIndexPayload>> response = bookServiceClient.getBooks(0, size);
        if (response == null || response.getResult() == null) {
            return 0;
        }
        PageResponse<BookIndexPayload> firstPage = response.getResult();

        int totalPage = firstPage.getTotalPages();
        for (int page = 0; page < totalPage; page++) {
            PageResponse<BookIndexPayload> currentPage;
            if (page == 0) {
                currentPage = firstPage;
            } else {
                ApiResponse<PageResponse<BookIndexPayload>> pageResponse = bookServiceClient.getBooks(page, size);
                currentPage = pageResponse.getResult();
            }
            if (currentPage.getData() == null) {
                continue;
            }
            for (BookIndexPayload payload : currentPage.getData()) {
                upsert(payload);
                totalIndexed++;
            }
        }

        return totalIndexed;
    }
}
