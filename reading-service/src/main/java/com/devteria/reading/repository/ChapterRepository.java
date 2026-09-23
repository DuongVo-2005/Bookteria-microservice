package com.devteria.reading.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.reading.entity.Chapter;

public interface ChapterRepository extends MongoRepository<Chapter, String> {
    Page<Chapter> findByBookIdOrderByChapterNumberAsc(String bookId, Pageable pageable);

    Optional<Chapter> findByBookIdAndChapterNumber(String bookId, int chapterNumber);

    long countByBookId(String bookId);

    void deleteAllByBookId(String bookId);
}
