package com.devteria.book.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.book.entity.ReadingChallenge;

public interface ReadingChallengeRepository extends MongoRepository<ReadingChallenge, String> {
    Optional<ReadingChallenge> findByUserIdAndYear(String userId, int year);
}
