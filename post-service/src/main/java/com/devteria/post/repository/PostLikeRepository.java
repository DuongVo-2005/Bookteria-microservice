package com.devteria.post.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.post.entity.PostLike;

@Repository
public interface PostLikeRepository extends MongoRepository<PostLike, String> {
    boolean existsByPostIdAndUserId(String postId, String userId);

    void deleteByPostIdAndUserId(String postId, String userId);

    void deleteAllByPostId(String postId);
}
