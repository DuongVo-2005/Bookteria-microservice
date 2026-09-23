package com.devteria.group.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.group.entity.GroupPostLike;

public interface GroupPostLikeRepository extends MongoRepository<GroupPostLike, String> {
    boolean existsByPostIdAndUserId(String postId, String userId);

    long countByPostId(String postId);

    void deleteByPostIdAndUserId(String postId, String userId);

    void deleteAllByPostIdIn(List<String> postIds);
}
