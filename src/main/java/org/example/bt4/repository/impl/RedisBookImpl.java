package org.example.bt4.repository.impl;

import org.example.bt4.model.Book;
import org.example.bt4.repository.BookRepository;
import org.example.bt4.repository.redis.RedisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import io.netty.util.internal.ThreadLocalRandom;

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
        if (book.getId() == null) {
            book.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        }
        
        book.setContent(null);
        redisRepository.save(book);
    }

    @Override
    public Page<Book> searchBooks(String title, String author, String content, int page, int size) {
        List<Book> allBooks = new ArrayList<>();
        redisRepository.findAll().forEach(allBooks::add);

        List<Book> filteredBooks = allBooks.stream()
                .filter(b -> (title == null || title.isBlank() || (b.getTitle() != null && b.getTitle().toLowerCase().contains(title.toLowerCase()))))
                .filter(b -> (author == null || author.isBlank() || (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(author.toLowerCase()))))
                .collect(Collectors.toList());

        int start = page * size;
        int end = Math.min(start + size, filteredBooks.size());

        if(start >= filteredBooks.size() || start < 0){
            return Page.empty(PageRequest.of(page, size));
        }

        List<Book> sub = filteredBooks.subList(start, end);
        return new PageImpl<>(sub, PageRequest.of(page, size), filteredBooks.size());
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
        List<Long> longIds = ids.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        redisRepository.deleteAllById(longIds); 
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {
        List<Book> allBooks = new ArrayList<>();
        redisRepository.findAll().forEach(allBooks::add);

        long count = allBooks.stream()
                .filter(b -> author.equalsIgnoreCase(b.getAuthor()))
                .count();

        if (count == 0) {
            return Map.of("message", "No statistics found for author: " + author);
        }

        return Map.of(
                "author", author,
                "totalBooks", count
        );
    }

    @Override
    public List<Book> findAllPaging(int page, int size) {
    
        List<Book> books = new ArrayList<>();
        redisRepository.findAll().forEach(books::add);

        int start = page * size;
        int end = Math.min(start + size, books.size());

        if(start >= books.size() || start < 0){
            return Collections.emptyList();
        }

        return books.subList(start, end);
    }
}
