package com.devteria.group.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.devteria.group.dto.GroupPostStatus;
import com.devteria.group.entity.GroupPost;

public interface GroupPostRepository extends MongoRepository<GroupPost, String> {
    Page<GroupPost> findByGroupIdOrderByCreatedAtDesc(String groupId, Pageable pageable);

    // idea-spec BA OPS-01: feed công khai. Bug thật bắt được lúc verify live: field initializer
    // "GroupPostStatus status = PUBLISHED" trên entity chỉ ảnh hưởng cách Spring Data DESERIALIZE
    // 1 document đã lấy về, KHÔNG ảnh hưởng gì tới việc Mongo server lọc document theo query filter
    // — {status: "PUBLISHED"} không khớp document nào thiếu hẳn key "status" (10 post cũ trước khi
    // có field này). Dùng $or để coi thiếu key = PUBLISHED, tránh phải chạy migration backfill
    // Mongo (project chưa có Mongock/tool migration cho Mongo) mà vẫn không mất bài viết cũ khỏi feed.
    @Query(
            value = "{ 'groupId': ?0, $or: [ { 'status': 'PUBLISHED' }, { 'status': { '$exists': false } } ] }",
            sort = "{ 'createdAt': -1 }")
    Page<GroupPost> findPublishedByGroupId(String groupId, Pageable pageable);

    // idea-spec BA OPS-01: hàng chờ duyệt của OWNER/ADMIN — không cần xử lý thiếu key vì post cũ
    // (trước tính năng này) chưa từng ở trạng thái PENDING_APPROVAL.
    Page<GroupPost> findByGroupIdAndStatusOrderByCreatedAtDesc(
            String groupId, GroupPostStatus status, Pageable pageable);

    List<GroupPost> findByGroupId(String groupId);

    void deleteAllByGroupId(String groupId);

    // idea-spec Phase 6 - "20. Popular Groups" (recent activity signal)
    long countByGroupIdAndCreatedAtAfter(String groupId, Instant after);

    // idea-spec BA GAP-02: tìm mọi GroupPost đính kèm 1 sách khi sách đó bị xoá ở book-service.
    List<GroupPost> findAllByBookRef_BookId(String bookId);
}
