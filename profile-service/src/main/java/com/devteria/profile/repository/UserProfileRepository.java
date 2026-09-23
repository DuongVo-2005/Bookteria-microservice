package com.devteria.profile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.stereotype.Repository;

import com.devteria.profile.entity.UserProfile;

@Repository
public interface UserProfileRepository
        extends Neo4jRepository<UserProfile, String>, CypherdslConditionExecutor<UserProfile> {
    Optional<UserProfile> findByUserId(String id);

    Optional<UserProfile> findByUsername(String username);

    List<UserProfile> findAllByUserIdIn(List<String> userIds);

    Page<UserProfile> findAllByUsernameContainingIgnoreCase(String userName, Pageable pageable);
}
