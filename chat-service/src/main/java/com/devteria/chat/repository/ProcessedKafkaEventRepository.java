package com.devteria.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.chat.entity.ProcessedKafkaEvent;

public interface ProcessedKafkaEventRepository extends MongoRepository<ProcessedKafkaEvent, String> {}
