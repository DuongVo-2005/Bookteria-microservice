package com.devteria.reading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.reading.entity.ReadingProgress;

public interface ReadingProgressRepository extends MongoRepository<ReadingProgress, String> {
    Optional<ReadingProgress> findByUserIdAndBookId(String userId, String bookId);

    List<ReadingProgress> findByUserIdOrderByLastReadAtDesc(String userId);

    void deleteAllByBookId(String bookId);
}
