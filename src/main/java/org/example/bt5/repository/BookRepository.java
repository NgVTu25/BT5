package org.example.bt5.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface BookRepository<T, ID> {
    void saveBook(T book);

    Page<T> searchBooks(String title, String author, String content, Pageable pageable);

    void updateBook(ID id, T book);

    void deleteBooks(List<ID> ids);

    Map<String, Object> statisticByAuthor(String author);

    Page<T> findAllPaging(Pageable pageable);
}
