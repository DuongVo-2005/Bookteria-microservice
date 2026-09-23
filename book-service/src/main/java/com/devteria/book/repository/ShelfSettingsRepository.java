package com.devteria.book.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.book.entity.ShelfSettings;

@Repository
public interface ShelfSettingsRepository extends MongoRepository<ShelfSettings, String> {}
