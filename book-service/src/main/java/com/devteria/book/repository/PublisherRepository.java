package com.devteria.book.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.book.entity.Publisher;

@Repository
public interface PublisherRepository extends MongoRepository<Publisher, String> {
    Optional<Publisher> findByNameIgnoreCase(String name);
}
