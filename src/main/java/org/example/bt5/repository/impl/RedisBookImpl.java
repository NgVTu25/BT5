package org.example.bt5.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.bt5.model.BookCache;
import org.example.bt5.repository.BookRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository("redis")
@RequiredArgsConstructor
public class RedisBookImpl implements BookRepository<BookCache, String> {

    private static final String BOOKS_ALL_KEY = "books_db:all";

    private final RedisTemplate<String, BookCache> redisTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private String bookKey(String id) {
        return "book:" + id;
    }

    private String authorStatsKey(String author) {
        return "stats:author:" + normalize(author);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private void addIndexes(RedisOperations<String, String> ops, BookCache book) {
        if (book == null || book.getId() == null) return;
        String id = book.getId();

        if (book.getAuthor() != null && !book.getAuthor().isBlank()) {
            String authorKey = "idx:author:" + normalize(book.getAuthor()).replace(" ", "_");
            ops.opsForSet().add(authorKey, id);

            ops.opsForHash().increment(authorStatsKey(book.getAuthor()), "total", 1);
        }

        if (book.getTitle() != null && !book.getTitle().isBlank()) {
            String[] words = normalize(book.getTitle()).split("\\s+");
            for (String word : words) {
                ops.opsForSet().add("idx:title:" + word, id);
            }
        }
    }

    private void removeIndexes(BookCache book) {
        if (book == null || book.getId() == null) return;
        String id = book.getId();

        if (book.getAuthor() != null && !book.getAuthor().isBlank()) {
            String authorKey = "idx:author:" + normalize(book.getAuthor()).replace(" ", "_");
            stringRedisTemplate.opsForSet().remove(authorKey, id);
            if (book.getCategory() != null && !book.getCategory().isBlank()) {
                stringRedisTemplate.opsForHash().increment(authorStatsKey(book.getAuthor()), "category:" + book.getCategory(), -1);
            }
        }

        if (book.getTitle() != null && !book.getTitle().isBlank()) {
            String[] words = normalize(book.getTitle()).split("\\s+");
            for (String word : words) {
                stringRedisTemplate.opsForSet().remove("idx:title:" + word, id);
            }
        }
    }


    @Override
    public void saveBook(BookCache book) {
        if (book.getId() == null) {
            book.setId(UUID.randomUUID().toString());
        }
        book.setContent(null);

        String key = bookKey(book.getId());
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return;
        }

        redisTemplate.opsForValue().set(key, book);
        stringRedisTemplate.opsForList().rightPush(BOOKS_ALL_KEY, book.getId());
        addIndexes(stringRedisTemplate, book);
    }

    @Override
    public Page<BookCache> searchBooks(String title, String author, String Content, Pageable pageable) {
        List<String> indexKeys = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            for (String word : normalize(title).split("\\s+")) {
                indexKeys.add("idx:title:" + word);
            }
        }
        if (author != null && !author.isBlank()) {
            indexKeys.add("idx:author:" + normalize(author).replace(" ", "_"));
        }

        if (indexKeys.isEmpty()) return findAll(pageable);

        Set<String> resultIds;
        if (indexKeys.size() == 1) {
            resultIds = stringRedisTemplate.opsForSet().members(indexKeys.getFirst());
        } else {
            resultIds = stringRedisTemplate.opsForSet().intersect(indexKeys.getFirst(), indexKeys.subList(1, indexKeys.size()));
        }

        if (resultIds == null || resultIds.isEmpty()) return Page.empty(pageable);

        List<String> idList = new ArrayList<>(resultIds);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), idList.size());

        if (start >= idList.size()) return Page.empty(pageable);

        List<String> pageIds = idList.subList(start, end);

        return buildPageFromIds(pageIds, pageable, idList.size());
    }

    @Override
    public void updateBook(String id, BookCache book) {
        String key = bookKey(id);
        BookCache oldBook = redisTemplate.opsForValue().get(key);

        if (oldBook == null) {
            System.out.println("Không tìm thấy sách với ID: " + id);
            return;
        }

        book.setId(id);
        book.setContent(null);

        removeIndexes(oldBook);
        redisTemplate.opsForValue().set(key, book);
        addIndexes(stringRedisTemplate, book);
    }

    @Override
    public void deleteBooks(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;

        for (String id : ids) {
            String key = bookKey(id);
            BookCache book = redisTemplate.opsForValue().get(key);

            if (book != null) {
                stringRedisTemplate.opsForList().remove(BOOKS_ALL_KEY, 0, id);
                redisTemplate.delete(key);
                removeIndexes(book);
            }
        }
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {
        Map<Object, Object> hashData = stringRedisTemplate.opsForHash().entries(authorStatsKey(author));

        if (hashData.isEmpty()) {
            return Map.of("message", "Không có thống kê cho tác giả: " + author);
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Long> categoryStats = new HashMap<>();

        hashData.forEach((k, v) -> {
            String key = k.toString();
            Long value = Long.parseLong(v.toString());

            if ("total".equals(key)) {
                result.put("totalBooks", value);
            } else if (key.startsWith("category:")) {
                categoryStats.put(key.replace("category:", ""), value);
            }
        });

        result.put("author", author);
        result.put("categoryStats", categoryStats);
        return result;
    }

    @Override
    public Page<BookCache> findAll(Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = start + pageable.getPageSize() - 1;

        List<String> ids = stringRedisTemplate.opsForList().range(BOOKS_ALL_KEY, start, end);
        Long total = stringRedisTemplate.opsForList().size(BOOKS_ALL_KEY);

        return buildPageFromIds(ids, pageable, total != null ? total : 0);
    }

    @Override
    public void saveAll(List<BookCache> books) {
        if (books == null || books.isEmpty()) return;

        for (BookCache book : books) {
            if (book.getId() == null) book.setId(UUID.randomUUID().toString());
            book.setContent(null);
        }

        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for (BookCache book : books) {
                    operations.opsForValue().set(bookKey(book.getId()), book);
                }
                return null;
            }
        });

        stringRedisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for (BookCache book : books) {
                    operations.opsForList().rightPush(BOOKS_ALL_KEY, book.getId());
                    addIndexes(operations, book);
                }
                return null;
            }
        });
    }

    private Page<BookCache> buildPageFromIds(List<String> ids, Pageable pageable, long total) {
        if (ids == null || ids.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, total);
        }

        List<String> keys = ids.stream().map(this::bookKey).collect(Collectors.toList());

        List<BookCache> books = redisTemplate.opsForValue().multiGet(keys);

        List<BookCache> validBooks = books != null ?
                books.stream().filter(Objects::nonNull).collect(Collectors.toList()) :
                new ArrayList<>();

        return new PageImpl<>(validBooks, pageable, total);
    }
}