package com.devteria.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.devteria.search.entity.BookDocument;

public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, String> {
    @Query("""
		{
			"bool": {
			"must": [
				{ "multi_match": { "query": "?0", "fields": ["title^3", "subtitle^2", "description"] } }
			]
			}
		}
		""")
    Page<BookDocument> searchByKeyword(String keyWord, Pageable pageable);

    Page<BookDocument> findByCategoryIdsContainingAndAuthorIdsContaining(
            String categoryId, String authorId, Pageable pageable);

    Page<BookDocument> findByCategoryIdsContaining(String categoryId, Pageable pageable);

    Page<BookDocument> findByAuthorIdsContaining(String authorId, Pageable pageable);

    // Autocomplete: match_phrase_prefix trên title — khớp theo tiền tố cụm từ, đúng kiểu
    // gõ-tới-đâu-gợi-ý-tới-đó, khác search chính (multi_match toàn văn trên title/subtitle/description).
    @Query("""
			{
				"bool": {
				"must": [
					{ "match_phrase_prefix": { "title": "?0" } }
				]
				}
			}
			""")
    Page<BookDocument> suggestByTitlePrefix(String prefix, Pageable pageable);
}
