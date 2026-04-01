package org.example.bt5.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.bt5.model.BookCache;
import org.example.bt5.repository.BookRepository;
import org.example.bt5.repository.redis.RedisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository("Redis")
@RequiredArgsConstructor
public class RedisBookImpl implements BookRepository<BookCache, String> {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    private final RedisRepository redisRepository;

    @Override
    public void saveBook(BookCache book) {
        if (book.getId() == null) {
            book.setId(UUID.randomUUID().toString());
        }

        book.setContent(null);
        redisRepository.save(book);
    }

    @Override
    public Page<BookCache> searchBooks(String title, String author, String content, Pageable pageable) {
        int page = pageable == null ? 0 : pageable.getPageNumber();
        int size = pageable == null ? 10 : pageable.getPageSize();
        List<BookCache> allBooks = new ArrayList<>();
        redisRepository.findAll().forEach(allBooks::add);

        List<BookCache> filteredBooks = allBooks.stream().filter(b -> (title == null || title.isBlank() || (b.getTitle() != null && b.getTitle().toLowerCase().contains(title.toLowerCase())))).filter(b -> (author == null || author.isBlank() || (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(author.toLowerCase())))).collect(Collectors.toList());


        if (page >= filteredBooks.size() || page < 0) {
            return Page.empty(PageRequest.of(page, size));
        }

        List<BookCache> sub = filteredBooks.subList(page, size);
        return new PageImpl<>(sub, PageRequest.of(page, size), filteredBooks.size());
    }

    @Override
    public void updateBook(String id, BookCache book) {
        if (redisRepository.existsById(id)) {
            book.setId(id);
            redisRepository.save(book);
        }

    }


    @Override
    public void deleteBooks(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        redisRepository.deleteAllById(ids);
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {
        List<BookCache> allBooks = new ArrayList<>();
        redisRepository.findAll().forEach(allBooks::add);

        long count = allBooks.stream().filter(b -> author.equalsIgnoreCase(b.getAuthor())).count();

        if (count == 0) {
            return Map.of("message", "No statistics found for author: " + author);
        }

        return Map.of("author", author, "totalBooks", count);
    }

    @Override
    public Page<BookCache> findAllPaging(Pageable pageable) {

        List<BookCache> books = new ArrayList<>();
        redisRepository.findAll().forEach(books::add);

        int page = pageable == null ? 0 : pageable.getPageNumber();
        int size = pageable == null ? 10 : pageable.getPageSize();

        if (page >= books.size() || page < 0) {
            return Page.empty(PageRequest.of(page, size));
        }

        return new PageImpl<>(books.subList(page, Math.min(page + size, books.size())), PageRequest.of(page, size), books.size());
    }
}
