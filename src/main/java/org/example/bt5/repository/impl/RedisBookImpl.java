package org.example.bt5.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.bt5.model.BookCache;
import org.example.bt5.repository.BookRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private String authorBooksKey(String author) {
        return "idx:author:" + normalize(author).replace(" ", "_");
    }

    private String titleBooksKey(String title) {
        return "idx:title:" + normalize(title);
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
            String statsKey = authorStatsKey(book.getAuthor());

            ops.opsForSet().add(authorBooksKey(book.getAuthor()), id);

            ops.opsForHash().increment(statsKey, "total", 1);

            if (book.getCategory() != null && !book.getCategory().isBlank()) {
                String categoryKey = "category:" + normalize(book.getCategory());
                ops.opsForHash().increment(statsKey, categoryKey, 1);
            }
        }

        if (book.getTitle() != null && !book.getTitle().isBlank()) {
            String[] words = normalize(book.getTitle()).split("\\s+");
            for (String word : words) {
                if (!word.isBlank()) {
                    ops.opsForSet().add(titleBooksKey(word), id);
                }
            }
        }
    }

    private void removeIndexes(BookCache book) {
        if (book == null || book.getId() == null) return;
        String id = book.getId();

        if (book.getTitle() != null && !book.getTitle().isBlank()) {
            String[] words = normalize(book.getTitle()).split("\\s+");
            for (String word : words) {
                stringRedisTemplate.opsForSet().remove(titleBooksKey(word), id);
            }
        }

        if (book.getAuthor() != null && !book.getAuthor().isBlank()) {
            String statsKey = authorStatsKey(book.getAuthor());

            stringRedisTemplate.opsForSet().remove(authorBooksKey(book.getAuthor()), id);

            // total
            stringRedisTemplate.opsForHash().increment(statsKey, "total", -1);

            if (book.getCategory() != null && !book.getCategory().isBlank()) {
                String categoryKey = "category:" + normalize(book.getCategory());
                stringRedisTemplate.opsForHash().increment(statsKey, categoryKey, -1);
            }
        }
    }


    @Override
    public Object saveBook(BookCache book) {
        if (book.getId() == null) {
            book.setId(UUID.randomUUID().toString());
        }
        book.setContent(null);

        String key = bookKey(book.getId());
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return null;
        }

        redisTemplate.opsForValue().set(key, book);
        stringRedisTemplate.opsForList().rightPush(BOOKS_ALL_KEY, book.getId());
        addIndexes(stringRedisTemplate, book);
        return book;
    }

    @Override
    public Page<BookCache> searchBooks(String title, String author, String content, Pageable pageable) {
        List<String> indexKeys = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            for (String word : normalize(title).split("\\s+")) {
                if (!word.isBlank()) indexKeys.add(titleBooksKey(word));
            }
        }

        if (author != null && !author.isBlank()) {
            indexKeys.add(authorBooksKey(author));
        }

        if (indexKeys.isEmpty()) return findAll(pageable);

        Set<String> resultIds;
        if (indexKeys.size() == 1) {
            resultIds = stringRedisTemplate.opsForSet().members(indexKeys.getFirst());
        } else {
            String firstKey = indexKeys.getFirst();
            List<String> otherKeys = indexKeys.subList(1, indexKeys.size());
            resultIds = stringRedisTemplate.opsForSet().intersect(firstKey, otherKeys);
        }

        if (resultIds == null || resultIds.isEmpty()) return Page.empty(pageable);

        List<String> idList = new ArrayList<>(resultIds);
        Collections.sort(idList);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), idList.size());

        if (start >= idList.size()) return Page.empty(pageable);

        List<String> pageIds = idList.subList(start, end);
        return buildPageFromIds(pageIds, pageable, idList.size());
    }

    @Override
    public Boolean updateBook(String id, BookCache book) {
        String key = bookKey(id);
        BookCache oldBook = redisTemplate.opsForValue().get(key);

        if (oldBook == null) {
            System.out.println("Không tìm thấy sách với ID: " + id);
            return false;
        }

        book.setId(id);
        book.setContent(null);
        book.setCreateDate(oldBook.getCreateDate());

        removeIndexes(oldBook);
        redisTemplate.opsForValue().set(key, book);
        addIndexes(stringRedisTemplate, book);
        return true;
    }

    @Override
    public Boolean deleteBooks(List<String> ids) {
        if (ids == null || ids.isEmpty()) return false;

        for (String id : ids) {
            String key = bookKey(id);
            BookCache book = redisTemplate.opsForValue().get(key);

            if (book != null) {
                stringRedisTemplate.opsForList().remove(BOOKS_ALL_KEY, 0, id);
                redisTemplate.delete(key);
                System.out.println("Đã xóa sách với ID: " + id);
                removeIndexes(book);
            }
        }
        return true;
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
            System.out.println("KEY = " + k + " | VALUE = " + v);
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
            public Object execute(@NonNull RedisOperations operations) throws DataAccessException {
                for (BookCache book : books) {
                    operations.opsForList().rightPush(BOOKS_ALL_KEY, book.getId());
                    addIndexes(operations, book);
                }
                return null;
            }
        });
    }

    @Override
    public Object findById(String s) {
        String key = bookKey(s);

        Object result = redisTemplate.opsForValue().get(key);

        System.out.println("Searching Redis with key: " + key);
        System.out.println("Result found: " + (result != null));

        return result;
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