package com.devteria.group.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.group.entity.ProcessedKafkaEvent;

public interface ProcessedKafkaEventRepository extends MongoRepository<ProcessedKafkaEvent, String> {}
