package com.devteria.friend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.friend.entity.Block;

public interface BlockRepository extends MongoRepository<Block, String> {
    Optional<Block> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

    void deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);

    List<Block> findByBlockerId(String blockerId);
}
