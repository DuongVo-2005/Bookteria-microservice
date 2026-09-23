package com.devteria.book.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.book.entity.Book;

public interface BookRepository extends MongoRepository<Book, String> {
    Optional<Book> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByIsbn13(String isbn13);

    Page<Book> findAllByCategories_CategoryIdAndAuthors_AuthorId(String categoryId, String authorId, Pageable pageable);

    Page<Book> findAllByCategories_CategoryId(String categoryId, Pageable pageable);

    Page<Book> findAllByAuthors_AuthorId(String authorId, Pageable pageable);

    List<Book> findAllByCategories_CategoryId(String categoryId);

    List<Book> findAllByAuthors_AuthorId(String authorId);

    Page<Book> findAllBy(TextCriteria criteria, Pageable pageable);

    Optional<Book> findByGoogleBookId(String googleBookId);

    boolean existsByGoogleBookId(String googleBookId);

    // idea-spec BA v2 P1-02 follow-up (Phase 1 report, Known Issue #3): chặn xoá Author/Category/
    // Publisher đang được sách nào tham chiếu, tránh để lại dangling reference.
    boolean existsByAuthors_AuthorId(String authorId);

    boolean existsByCategories_CategoryId(String categoryId);

    boolean existsByPublisher_PublisherId(String publisherId);
}
