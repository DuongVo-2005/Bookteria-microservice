package com.devteria.book.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.book.entity.UserBookPreference;

public interface UserBookPreferenceRepository extends MongoRepository<UserBookPreference, String> {}
