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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {
    private static final Map<String, Class<?>> MODEL_MAPPING = Map.of(
            "mysql", BookSQL.class,
            "sql", BookSQL.class,
            "mongo", BookDocument.class,
            "mongodb", BookDocument.class,
            "redis", BookCache.class,
            "cache", BookCache.class,
            "influx", BookMetric.class
    );
    private final Map<String, BookRepository> bookRepositories;
    private final ObjectMapper objectMapper;

    private Class<?> getModelClass(String dbType) {
        return MODEL_MAPPING.entrySet().stream()
                .filter(entry -> dbType.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Model cho DB: " + dbType));
    }

    private BookRepository getRepo(String dbType) {

        BookRepository repo = bookRepositories.get(dbType);
        if (repo == null) {
            throw new IllegalArgumentException("Database không hợp lệ: " + dbType);
        }
        return repo;
    }

    public void saveBook(Object book, String dbType) {
        Object bookObj = convertToMappedObject(book, dbType);
        getRepo(dbType.toLowerCase()).saveBook(bookObj);
    }

    public Page<?> searchBooks(String dbType, String title, String author, String content, Pageable pageable) {
        return getRepo(dbType.toLowerCase()).searchBooks(title, author, content, pageable);
    }

    public Map<String, Object> statisticByAuthor(String dbType, String author) {
        return getRepo(dbType.toLowerCase()).statisticByAuthor(author);
    }

    public boolean updateBook(String dbType, String id, Object book) {
        Object bookObj = convertToMappedObject(book, dbType);
        if (dbType.contains("mysql") || dbType.contains("sql")) {
            return getRepo(dbType.toLowerCase()).updateBook(Long.parseLong(id), bookObj);
        } else {
            return getRepo(dbType.toLowerCase()).updateBook(id, bookObj);
        }
    }

    public boolean deleteBooks(String dbType, List<String> ids) {
        if (dbType.contains("mysql") || dbType.contains("sql")) {
            List<Long> BookIds = ids.stream().map(Long::parseLong).collect(Collectors.toList());
            return getRepo(dbType.toLowerCase()).deleteBooks(BookIds);
        } else {
            return getRepo(dbType.toLowerCase()).deleteBooks(ids);
        }
    }

    public Page<?> findAllPaging(String db, Pageable pageable) {
        int size = (pageable.getPageSize() <= 0) ? 10 : pageable.getPageSize();
        int page = Math.max(pageable.getPageNumber(), 0);

        Pageable safePageable = PageRequest.of(page, size, pageable.getSort());
        System.out.println(db.toLowerCase());
        return getRepo(db.toLowerCase()).findAll(safePageable);
    }

    private Object convertToMappedObject(Object rawBook, String dbType) {
        if (rawBook == null) return null;

        String type = dbType.toLowerCase().trim();

        Class<?> targetClass = getModelClass(type);

        return objectMapper.convertValue(rawBook, targetClass);

    }

}