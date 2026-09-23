package com.devteria.book.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.book.entity.Author;

@Repository
public interface AuthorRepository extends MongoRepository<Author, String> {
    Optional<Author> findByNameIgnoreCase(String name);
}
