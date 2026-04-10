package org.example.bt5;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bt5.model.BookCache;
import org.example.bt5.model.BookDocument;
import org.example.bt5.model.BookMetric;
import org.example.bt5.model.BookSQL;
import org.example.bt5.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = Bt5Application.class)
public class DatabasePerformanceTest {


    @Autowired
    private Map<String, BookRepository> bookRepositories;

    @Autowired
    private ObjectMapper objectMapper;

    public void generateAndInsert(String dbType) {
        int totalRecords = 500_000;
        int batchSize = 2000;

        String[] categories = {"Hành động", "Tình cảm", "Khoa học", "Lịch sử", "Kinh dị", "Trinh thám", "Kỹ năng", "Kinh tế"};
        String[] authors = {"Nguyen Van A", "Tran Thi B", "Le Van C", "Tolkien", "J.K. Rowling", "George Orwell", "Haruki Murakami"};

        System.out.println("Bắt đầu sinh 1.000.000 dữ liệu cho " + dbType);
        long start = System.currentTimeMillis();
        Instant now = Instant.now();

        List<Object> batch = new ArrayList<>(batchSize);

        for (int i = 1; i <= totalRecords; i++) {
            Object book = createBookModel(dbType, i, authors, categories, now);
            batch.add(book);

            if (i % batchSize == 0) {
                saveBatch(dbType, batch);
                batch.clear();
                System.out.println("Đã xử lý: " + i + " cuốn...");
            }
        }

        if (!batch.isEmpty()) {
            saveBatch(dbType, batch);
        }

        long end = System.currentTimeMillis();
        System.out.println("Hoàn thành trong: " + (end - start) / 1000 + " giây.");
    }

    // ================= CREATE DATA =================
    private Object createBookModel(String dbType, int i,
                                   String[] authors, String[] categories,
                                   Instant now) {

        String author = authors[i % authors.length];
        String category = categories[i % categories.length];
        String title = "Tiêu đề " + i;
        String content = "Nội dung hấp dẫn " + i;

        if (dbType.equalsIgnoreCase("mysql")) {
            return BookSQL.builder()
                    .author(author)
                    .category(category)
                    .title(title)
                    .content(content)
                    .createDate(now.plusMillis(i))
                    .viewCount(0L)
                    .downloadCount(0L)
                    .build();
        } else {
            return BookDocument.builder()
                    .author(author)
                    .category(category)
                    .title(title)
                    .content(content)
                    .createDate(now.plusMillis(i))
                    .viewCount(0L)
                    .downloadCount(0L)
                    .build();
        }
    }

    private void saveBatch(String dbType, List<Object> batch) {
        BookRepository repo = getRepo(dbType);

        List<Object> convertedList = new ArrayList<>(batch.size());

        for (Object item : batch) {
            convertedList.add(convertToCorrectType(item, dbType));
        }

        repo.saveAll(convertedList);
    }

    public void saveBook(Object bookBody, String dbType) {
        Object concreteBook = convertToCorrectType(bookBody, dbType);
        getRepo(dbType).saveBook(concreteBook);
    }

    // ================= CONVERT =================
    private Object convertToCorrectType(Object bookBody, String dbType) {
        String type = dbType.toLowerCase();

        if (type.contains("mysql") || type.contains("sql")) {
            return objectMapper.convertValue(bookBody, BookSQL.class);
        } else if (type.contains("mongo")) {
            return objectMapper.convertValue(bookBody, BookDocument.class);
        } else if (type.contains("redis")) {
            return objectMapper.convertValue(bookBody, BookCache.class);
        } else if (type.contains("influx")) {
            return objectMapper.convertValue(bookBody, BookMetric.class);
        } else {
            throw new IllegalArgumentException("Database không hợp lệ: " + dbType);
        }
    }

    // ================= GET REPO =================
    private BookRepository getRepo(String dbType) {
        BookRepository repo = bookRepositories.get(dbType);

        if (repo == null) {
            throw new IllegalArgumentException("Database không hợp lệ: " + dbType);
        }

        return repo;
    }

    @Test
    public void runPerformanceTestAllDatabases() {
        System.out.println("========== BẮT ĐẦU PERFORMANCE TEST ==========");

        Map<String, Long> results = new HashMap<>();

        for (String dbType : bookRepositories.keySet()) {
            try {
                System.out.println("\n--- TEST DB: " + dbType + " ---");

                long start = System.currentTimeMillis();

                generateAndInsert(dbType);

                long end = System.currentTimeMillis();
                long duration = (end - start) / 1000;

                results.put(dbType, duration);

                System.out.println(">>> " + dbType + " hoàn thành trong " + duration + " giây");

            } catch (Exception e) {
                System.out.println("❌ Lỗi với DB: " + dbType + " - " + e.getMessage());
            }
        }

        printSummary(results);
    }

    private void printSummary(Map<String, Long> results) {
        System.out.println("\n========== KẾT QUẢ ==========");

        results.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry ->
                        System.out.println(entry.getKey() + " : " + entry.getValue() + " giây")
                );

        System.out.println("=================================");
    }
}