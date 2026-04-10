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

//    @Override
//    public Page<BookSQL> searchBooks(String title, String author, String content, Pageable pageable) {
//
//        Specification<BookSQL> spec = (root, query, cb) -> cb.conjunction();
//
//        if (title != null && !title.isBlank()) {
//            spec = spec.and((root, query, cb) ->
//                    cb.like((root.get("title")), "%" + title.toLowerCase() + "%"));
//        }
//
//        if (author != null && !author.isBlank()) {
//            spec = spec.and((root, query, cb) ->
//                    cb.like((root.get("author")), "%" + author.toLowerCase() + "%"));
//        }
//
//        if (content != null && !content.isBlank()) {
//            spec = spec.and((root, query, cb) ->
//                    cb.like((root.get("content")), "%" + content.toLowerCase() + "%"));
//        }
//
//        pageable = PageRequest.of(
//                pageable == null ? 0 : pageable.getPageNumber(),
//                pageable == null ? 10 : pageable.getPageSize(),
//                Sort.by("author").ascending()
//        );
//
//        return sqlRepository.findAll(spec, pageable);
//    }

//    private String normalize(String input) {
//        return (input == null || input.isBlank()) ? null : input.trim();
//    }

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
    public void updateBook(Long id, BookSQL book) {
        BookSQL upBook = sqlRepository.findById(id).orElse(null);

        if (upBook != null) {
            upBook.setTitle(book.getTitle());
            upBook.setAuthor(book.getAuthor());
            upBook.setContent(book.getContent());
            upBook.setCategory(book.getCategory());
            upBook.setViewCount(book.getViewCount());

            sqlRepository.save(upBook);
        } else {
            System.err.println("[LỖI] Không tìm thấy sách với ID: " + id);
        }
    }

    @Override
    public void deleteBooks(List<Long> ids) {
        for (Long id : ids) {
            sqlRepository.deleteById(id);
        }
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {

        Map<String, Object> rawStats = sqlRepository.statisticByAuthor(author);

        if (rawStats == null || rawStats.isEmpty()) {
            return Map.of("message", "Không có dữ liệu cho tác giả: " + author);
        }
        return new HashMap<>(rawStats);
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
