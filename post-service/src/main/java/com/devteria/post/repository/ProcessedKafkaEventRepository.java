package com.devteria.post.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.post.entity.ProcessedKafkaEvent;

public interface ProcessedKafkaEventRepository extends MongoRepository<ProcessedKafkaEvent, String> {}
