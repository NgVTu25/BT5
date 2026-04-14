package org.example.bt5.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.bt5.model.BookSQL;
import org.example.bt5.repository.BookRepository;
import org.example.bt5.repository.sql.SqlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository("mysql")
@RequiredArgsConstructor
public class SqlBookImpl implements BookRepository<BookSQL, Long> {
    private final SqlRepository sqlRepository;

    @Override
    public void saveBook(BookSQL book) {
        book.setCreateDate(Instant.now());
        sqlRepository.save(book);
        System.out.println(book.getId());
    }

    @Override
    public Page<BookSQL> searchBooks(String title, String author, String content, Pageable pageable) {

        String keyword = buildKeyword(title, author, content);

        pageable = PageRequest.of(
                pageable == null ? 0 : pageable.getPageNumber(),
                pageable == null ? 10 : pageable.getPageSize()
        );

        if (keyword.isBlank()) {
            return sqlRepository.findAll(pageable);
        }

        return sqlRepository.searchFullText(keyword, pageable);
    }

    private String buildKeyword(String title, String author, String content) {
        StringBuilder sb = new StringBuilder();

        if (title != null && !title.isBlank()) {
            sb.append(title).append(" ");
        }
        if (author != null && !author.isBlank()) {
            sb.append(author).append(" ");
        }
        if (content != null && !content.isBlank()) {
            sb.append(content);
        }

        return sb.toString().trim();
    }

    @Override
    public Boolean updateBook(Long id, BookSQL book) {
        BookSQL upBook = sqlRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy sách với ID: " + id));

        if (upBook != null) {
            upBook.setTitle(book.getTitle() != null ? book.getTitle() : upBook.getTitle());
            upBook.setAuthor(book.getAuthor() != null ? book.getAuthor() : upBook.getAuthor());
            upBook.setContent(book.getContent() != null ? book.getContent() : upBook.getContent());
            upBook.setCategory(book.getCategory() != null ? book.getCategory() : upBook.getCategory());
            upBook.setViewCount(book.getViewCount() != null ? book.getViewCount() : upBook.getViewCount());

            sqlRepository.save(upBook);
            return true;
        } else {
            System.err.println("[LỖI] Không tìm thấy sách với ID: " + id);
        }
        return false;
    }

    @Override
    public Boolean deleteBooks(List<Long> ids) {
        for (Long id : ids) {
            if (!sqlRepository.existsById(id))
                return false;
            sqlRepository.deleteById(id);
        }
        return true;
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {

        List<Map<String, Object>> rawStats = sqlRepository.statisticByAuthor(author);

        if (rawStats == null || rawStats.isEmpty()) {
            return Map.of("message", "Không có dữ liệu cho tác giả: " + author);
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Long> categoryStats = new HashMap<>();

        long total = 0;

        for (Map<String, Object> row : rawStats) {
            String category = (String) row.get("category");
            Long count = ((Number) row.get("total_books")).longValue();

            categoryStats.put(category, count);
            total += count;
        }

        result.put("author", author);
        result.put("total_books", total);
        result.put("categories", categoryStats);

        return result;
    }

    @Override
    public Page<BookSQL> findAll(Pageable pageable) {
        Pageable actualPageable = (pageable != null) ? pageable : PageRequest.of(0, 10);

        return sqlRepository.findAll(actualPageable);
    }

    @Override
    public void saveAll(List<BookSQL> books) {
        sqlRepository.saveAll(books);
    }

}
