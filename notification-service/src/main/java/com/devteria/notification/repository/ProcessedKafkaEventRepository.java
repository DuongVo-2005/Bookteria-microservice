package com.devteria.notification.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.notification.entity.ProcessedKafkaEvent;

public interface ProcessedKafkaEventRepository extends MongoRepository<ProcessedKafkaEvent, String> {}
