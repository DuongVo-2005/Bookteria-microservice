package com.devteria.reading.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.reading.entity.ReadingStreak;

public interface ReadingStreakRepository extends MongoRepository<ReadingStreak, String> {}
