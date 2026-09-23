package com.devteria.file.repository;

import java.time.Instant;
import java.util.List;

import com.devteria.file.entity.FileMgmt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileMgmtRepository extends MongoRepository<FileMgmt, String> {
    // idea-spec BA GAP-05
    List<FileMgmt> findAllByAttachedFalseAndCreatedAtBefore(Instant threshold);
}
