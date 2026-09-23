package com.devteria.group.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.group.entity.GroupPostComment;

public interface GroupPostCommentRepository extends MongoRepository<GroupPostComment, String> {
    List<GroupPostComment> findByPostIdOrderByCreatedAtAsc(String postId);

    long countByPostId(String postId);

    void deleteAllByPostIdIn(List<String> postIds);
}
