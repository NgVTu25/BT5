package org.example.bt5.service;

import lombok.RequiredArgsConstructor;
import org.example.bt5.model.BookCache;
import org.example.bt5.model.BookDocument;
import org.example.bt5.model.BookMetric;
import org.example.bt5.model.BookSQL;
import org.example.bt5.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookService<T> {
    private final Map<String, BookRepository> bookRepositories;
    private final ObjectMapper objectMapper;

    private BookRepository getRepo(String dbType) {

        BookRepository repo = bookRepositories.get(dbType);
        if (repo == null) {
            throw new IllegalArgumentException("Database không hợp lệ: " + dbType);
        }
        return repo;
    }

    public void saveBook(Object book, String dbType) {
        Object Bookdb;

        String type = (dbType == null) ? "" : dbType.trim().toLowerCase();

        try {
            Object bookDb;

            if (type.contains("mysql") || type.contains("sql")) {
                bookDb = objectMapper.convertValue(book, BookSQL.class);
            } else if (type.contains("mongo")) {
                bookDb = objectMapper.convertValue(book, BookDocument.class);
            } else if (type.contains("redis") || type.contains("cache")) {
                bookDb = objectMapper.convertValue(book, BookCache.class);
            } else if (type.contains("influx")) {
                bookDb = objectMapper.convertValue(book, BookMetric.class);
            } else {
                throw new IllegalArgumentException("Database type không được hỗ trợ: " + dbType);
            }

            getRepo(type).saveBook(bookDb);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi chuyển đổi dữ liệu sách: " + e.getMessage());
        }
    }

    public Page<?> searchBooks(String dbType, String title, String author, String content, Pageable pageable) {
        return getRepo(dbType.toLowerCase()).searchBooks(title, author, content, pageable);
    }

    public Map<String, Object> statisticByAuthor(String dbType, String author) {
        return getRepo(dbType.toLowerCase()).statisticByAuthor(author);
    }

    public void updateBook(String dbType, String id, T book) {
        getRepo(dbType.toLowerCase()).updateBook(id, book);
    }

    public void deleteBooks(String dbType, List<String> ids) {
        getRepo(dbType.toLowerCase()).deleteBooks(ids);
    }

    public Page<?> findAllPaging(String db, Pageable pageable) {
        int size = (pageable.getPageSize() <= 0) ? 10 : pageable.getPageSize();
        int page = Math.max(pageable.getPageNumber(), 0);

        Pageable safePageable = PageRequest.of(page, size, pageable.getSort());
        System.out.println(db.toLowerCase());
        return getRepo(db.toLowerCase()).findAll(safePageable);
    }
}