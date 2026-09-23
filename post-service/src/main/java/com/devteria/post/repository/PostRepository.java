package com.devteria.post.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.post.dto.PostVisibility;
import com.devteria.post.entity.Post;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {

    Page<Post> findAllByUserId(String userId, Pageable pageable);

    // idea-spec Phase 3 - "5. Global Feed"
    Page<Post> findAllByVisibilityOrderByCreatedDateDesc(PostVisibility visibility, Pageable pageable);

    // idea-spec Phase 3 - "5. Global Feed" (feed theo bạn bè)
    Page<Post> findAllByUserIdInAndVisibilityInOrderByCreatedDateDesc(
            List<String> userIds, List<PostVisibility> visibilities, Pageable pageable);

    // Xem bài viết của 1 user cụ thể, đã lọc theo visibility phù hợp với viewer.
    Page<Post> findAllByUserIdAndVisibilityInOrderByCreatedDateDesc(
            String userId, List<PostVisibility> visibilities, Pageable pageable);

    // idea-spec Phase 6 - "22. Personalized Feed" (tín hiệu "theo sách quan tâm")
    Page<Post> findAllByBookIdInAndVisibilityOrderByCreatedDateDesc(
            List<String> bookIds, PostVisibility visibility, Pageable pageable);
}
