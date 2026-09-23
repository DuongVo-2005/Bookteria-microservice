package com.devteria.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.devteria.search.entity.GroupDocument;

public interface GroupSearchRepository extends ElasticsearchRepository<GroupDocument, String> {
    @Query("""
			{
				"bool": {
				"must": [
					{ "multi_match": { "query": "?0", "fields": ["name^2", "description"] } }
				]
				}
			}
			""")
    Page<GroupDocument> searchByKeyword(String keyword, Pageable pageable);

    Page<GroupDocument> findByCategory(String category, Pageable pageable);
}
