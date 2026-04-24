package org.example.bt5.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface PagingAndSortingRepository<T, ID> {
	Page<T> findAll(Pageable pageable);

	Page<T> searchBooks(String title, String author, String content, Pageable pageable);

	Map<String, Object> statisticByAuthor(String author);
}
