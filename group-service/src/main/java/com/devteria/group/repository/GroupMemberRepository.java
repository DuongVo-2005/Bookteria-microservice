package com.devteria.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.group.entity.GroupMember;

public interface GroupMemberRepository extends MongoRepository<GroupMember, String> {
    Optional<GroupMember> findByGroupIdAndUserId(String groupId, String userId);

    boolean existsByGroupIdAndUserId(String groupId, String userId);

    List<GroupMember> findByGroupId(String groupId);

    List<GroupMember> findByUserId(String userId);

    long countByGroupId(String groupId);

    void deleteByGroupIdAndUserId(String groupId, String userId);

    void deleteAllByGroupId(String groupId);
}
