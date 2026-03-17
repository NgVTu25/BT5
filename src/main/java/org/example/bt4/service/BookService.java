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

    public void saveBookToSpecificDB(Book book, String dbType) {
        BookRepository targetRepo = bookRepositories.get(dbType);

        if (targetRepo != null) {
            targetRepo.saveBook(book);
        } else {
            throw new IllegalArgumentException("Unknown database type: " + dbType);
        }
    }

    public Page<Book> searchBooks(String dbType , String title, String author, String content, int page, int size) {
        BookRepository targetRepo = bookRepositories.get(dbType);

        if (targetRepo != null) {
            targetRepo.searchBooks(title, author, content, page, size);
        } else {
            throw new IllegalArgumentException("Unknown database type: " + dbType);
        }
        return targetRepo.searchBooks(title, author, content, page, size);
    }

    public void updateBook(String dbType, Long id, Book book) {
        BookRepository targetRepo = bookRepositories.get(dbType);

        if (targetRepo != null) {
            targetRepo.updateBook(id, book);
        } else {
            throw new IllegalArgumentException("Unknown database type: " + dbType);
        }
    }

    public void deleteBooks(String dbType, List<String> ids) {
        BookRepository targetRepo = bookRepositories.get(dbType);

        if (targetRepo != null) {
            targetRepo.deleteBooks(ids);
        } else {
            throw new IllegalArgumentException("Unknown database type: " + dbType);
        }
    }

    public Map<String, Object> statisticByAuthor(String dbType, String author) {
        BookRepository targetRepo = bookRepositories.get(dbType);

        if (targetRepo != null) {
            return targetRepo.statisticByAuthor(author);
        } else {
            throw new IllegalArgumentException("Unknown database type: " + dbType);
        }
    }

    public List<Book> findAllPaging(String dbType, int page, int size) {
        BookRepository targetRepo = bookRepositories.get(dbType);

        if (targetRepo != null) {
            return targetRepo.findAllPaging(page, size);
        } else {
            throw new IllegalArgumentException("Unknown database type: " + dbType);
        }
    }
}