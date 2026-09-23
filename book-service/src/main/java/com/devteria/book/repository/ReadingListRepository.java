package com.devteria.book.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.book.dto.ReadingStatus;
import com.devteria.book.entity.ReadingList;

@Repository
public interface ReadingListRepository extends MongoRepository<ReadingList, String> {
    boolean existsByUserIdAndBookId(String userId, String bookId);

    ReadingList findByUserIdAndBookId(String userId, String bookId);

    Page<ReadingList> findAllByUserId(String userId, Pageable pageable);

    List<ReadingList> findAllByUserId(String userId);

    Page<ReadingList> findAllByUserIdAndStatus(String userId, ReadingStatus status, Pageable pageable);

    // idea-spec Phase 5 - "17. Reading Statistics"
    long countByUserIdAndStatus(String userId, ReadingStatus status);

    List<ReadingList> findAllByUserIdAndStatus(String userId, ReadingStatus status);

    // idea-spec BA GAP-02: tìm mọi shelf-entry của 1 sách khi sách bị xoá, để chuyển sang
    // UNAVAILABLE thay vì để trỏ tới 1 bookId không còn tồn tại.
    List<ReadingList> findAllByBookId(String bookId);
}
