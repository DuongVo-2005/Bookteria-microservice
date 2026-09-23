package com.devteria.reading.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.reading.entity.ProcessedKafkaEvent;

public interface ProcessedKafkaEventRepository extends MongoRepository<ProcessedKafkaEvent, String> {}
