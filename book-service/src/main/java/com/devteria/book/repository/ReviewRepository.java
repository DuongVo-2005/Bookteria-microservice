package com.devteria.book.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.book.entity.Review;

public interface ReviewRepository extends MongoRepository<Review, String> {
    Page<Review> findAllByBookId(String bookId, Pageable pageable);

    boolean existsByBookIdAndUserId(String bookId, String userId);

    // idea-spec BA OPS-02: Merge Books Tool — duyệt toàn bộ review của sách trùng để chuyển sang sách gốc.
    List<Review> findAllByBookId(String bookId);
}
