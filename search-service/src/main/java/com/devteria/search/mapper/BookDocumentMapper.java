package com.devteria.search.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.devteria.search.dto.BookIndexPayload;
import com.devteria.search.entity.BookDocument;

@Component
public class BookDocumentMapper {

    public BookDocument toDocument(BookIndexPayload payload) {

        List<String> authorNames = payload.getAuthors() == null
                ? List.of()
                : payload.getAuthors().stream()
                        .map(BookIndexPayload.AuthorInfo::getName)
                        .toList();

        List<String> authorIds = payload.getAuthors() == null
                ? List.of()
                : payload.getAuthors().stream()
                        .map(BookIndexPayload.AuthorInfo::getAuthorId)
                        .toList();

        List<String> categoryNames = payload.getCategories() == null
                ? List.of()
                : payload.getCategories().stream()
                        .map(BookIndexPayload.CategoryInfo::getName)
                        .toList();

        List<String> categoryIds = payload.getCategories() == null
                ? List.of()
                : payload.getCategories().stream()
                        .map(BookIndexPayload.CategoryInfo::getCategoryId)
                        .toList();

        String publisherName =
                payload.getPublisher() != null ? payload.getPublisher().getName() : null;

        String publisherId =
                payload.getPublisher() != null ? payload.getPublisher().getPublisherId() : null;

        String coverImage =
                payload.getMetadata() != null ? payload.getMetadata().getCoverImage() : null;

        BigDecimal ratingAverage =
                payload.getStats() != null && payload.getStats().getRatingAverage() != null
                        ? payload.getStats().getRatingAverage()
                        : BigDecimal.ZERO;

        Long ratingCount = payload.getStats() != null && payload.getStats().getRatingCount() != null
                ? payload.getStats().getRatingCount()
                : 0L;

        return BookDocument.builder()
                .id(payload.getId())
                .title(payload.getTitle())
                .subtitle(payload.getSubtitle())
                .slug(payload.getSlug())
                .isbn13(payload.getIsbn13())
                .description(payload.getDescription())
                .authorNames(authorNames)
                .authorIds(authorIds)
                .categoryNames(categoryNames)
                .categoryIds(categoryIds)
                .publisherName(publisherName)
                .publisherId(publisherId)
                .coverImage(coverImage)
                .ratingAverage(ratingAverage)
                .ratingCount(ratingCount)
                .status(payload.getStatus())
                .createdAt(payload.getCreatedAt())
                .build();
    }
}
