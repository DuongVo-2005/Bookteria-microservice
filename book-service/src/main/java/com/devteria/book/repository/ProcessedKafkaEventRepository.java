package com.devteria.book.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.book.entity.ProcessedKafkaEvent;

public interface ProcessedKafkaEventRepository extends MongoRepository<ProcessedKafkaEvent, String> {}
