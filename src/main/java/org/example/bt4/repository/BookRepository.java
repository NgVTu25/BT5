package org.example.bt4.repository;

import org.example.bt4.model.Book;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface BookRepository {
    void saveBook(Book book);

    Page<Book> searchBooks(String title, String author, String content, int page, int size);

    void updateBook(Long id, Book book);

    void deleteBooks(List<String> ids);

    Map<String, Object> statisticByAuthor(String author);

    List<Book> findAllPaging(int page, int size);
}
