package com.devteria.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.devteria.search.entity.PostDocument;

public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {
    @Query("""
			{
				"bool": {
				"must": [
					{ "match": { "content": "?0" } }
				]
				}
			}
			""")
    Page<PostDocument> searchByKeyword(String keyword, Pageable pageable);

    Page<PostDocument> findByHashtagsContaining(String hashtag, Pageable pageable);
}
