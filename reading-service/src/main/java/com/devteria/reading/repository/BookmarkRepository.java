package com.devteria.reading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.reading.entity.Bookmark;

public interface BookmarkRepository extends MongoRepository<Bookmark, String> {
    List<Bookmark> findByUserIdAndBookId(String userId, String bookId);

    Optional<Bookmark> findByIdAndUserId(String id, String userId);

    void deleteAllByBookId(String bookId);

    // idea-spec BA v2 §3.1 Offline Sync Batch: dedupe theo idempotency key client tự sinh.
    Optional<Bookmark> findByUserIdAndClientId(String userId, String clientId);
}
