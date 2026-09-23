package com.devteria.group.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.group.entity.Group;

public interface GroupRepository extends MongoRepository<Group, String> {
    Page<Group> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Group> findByIdIn(List<String> ids);

    // idea-spec Phase 6 - "20. Popular Groups"
    List<Group> findByCategory(String category);
}
