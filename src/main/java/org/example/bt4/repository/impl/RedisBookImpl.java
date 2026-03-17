package org.example.bt4.repository.impl;

import org.example.bt4.model.Book;
import org.example.bt4.repository.BookRepository;
import org.example.bt4.repository.RedisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository("Redis")
public class RedisBookImpl implements BookRepository {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    private final RedisRepository redisRepository;
    private final RedisTemplate<String, Book> redisTemplate;

    public RedisBookImpl(RedisRepository redisRepository, RedisTemplate<String, Book> redisTemplate) {
        this.redisRepository = redisRepository;
        this.redisTemplate = redisTemplate;

    }

    @Override
    public void saveBook(Book book) {
        book.setContent(null);
       redisRepository.save(book);
    }

    @Override
    public Page<Book> searchBooks(String title,  String author, String content, int page, int size) {

        List<Book> books = new ArrayList<>();
        redisRepository.findAll().forEach(books::add);

        int start = page * size;
        int end = Math.min(start + size, books.size());

        if(start > books.size()){
            return Page.empty();
        }

        List<Book> sub = books.subList(start, end);

        return new PageImpl<>(sub, PageRequest.of(page, size), books.size());
    }

    @Override
    public void updateBook(Long id, Book book) {
        book.setContent(null);
        if (redisRepository.existsById(id)) {
            book.setId(id);
            redisRepository.save(book);
        }

    }


    @Override
    public void deleteBooks(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        for(String id : ids){
            redisRepository.deleteById(Long.valueOf(id));
        }
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {
        String key = "author:stats:" + author;

        Map<Object, Object> stats = redisTemplate.opsForHash().entries(key);

        if (stats.isEmpty()) {
            return Map.of("message", "No statistics found for author: " + author);
        }

        return stats.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        Map.Entry::getValue
                ));
    }

    @Override
    public List<Book> findAllPaging(int page, int size) {

        List<Book> books = new ArrayList<>();
        redisRepository.findAll().forEach(books::add);

        int start = page * size;
        int end = Math.min(start + size, books.size());

        if(start > books.size()){
            return List.of();
        }

        return books.subList(start, end);
    }
}
