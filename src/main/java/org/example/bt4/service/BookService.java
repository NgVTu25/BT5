package org.example.bt4.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import org.example.bt4.model.Book;
import org.example.bt4.repository.BookRepository;

import java.util.List;
import java.util.Map;

@Service
public class BookService{
    private final Map<String, BookRepository> bookRepositories;

    public BookService(Map<String, BookRepository> bookRepositories) {
        this.bookRepositories = bookRepositories;
        System.out.println("Các Bean hiện có: " + bookRepositories.keySet());
    }

    private BookRepository getRepo(String dbType) {
        BookRepository repo = bookRepositories.get(dbType);
        if (repo == null) {
            throw new IllegalArgumentException("Unknown database type: " + dbType);
        }
        return repo;
    }

    public void saveBook(Book book, String dbType) {
        getRepo(dbType).saveBook(book);
    }

    public Page<Book> searchBooks(String dbType, String title, String author, String content, int page, int size) {
        return getRepo(dbType).searchBooks(title, author, content, page, size);
    }

    public void updateBook(String dbType, Long id, Book book) {
        getRepo(dbType).updateBook(id, book);
    }

    public void deleteBooks(String dbType, List<String> ids) {
        getRepo(dbType).deleteBooks(ids);
    }

    public Map<String, Object> statisticByAuthor(String dbType, String author) {
        return getRepo(dbType).statisticByAuthor(author);
    }

    public List<Book> findAllPaging(String dbType, int page, int size) {
        return getRepo(dbType).findAllPaging(page, size);
    }
}