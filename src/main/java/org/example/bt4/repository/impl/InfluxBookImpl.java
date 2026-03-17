package org.example.bt4.repository.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.example.bt4.model.Book;
import org.example.bt4.repository.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Repository("Influx")
public class InfluxBookImpl implements BookRepository {
    public final InfluxDBClient influxDBClient;

    @Value("${influx.url}")
    private String url;
    @Value("${influx.token}")
    private String token;
    @Value("${influx.org}")
    private String org;
    @Value("${influx.bucket}")
    private String bucket;

    private static final String MEASUREMENT = "Book";

    public InfluxBookImpl(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    private List<Book> mapFluxToBooks(String fluxQuery) {
        List<Book> books = new ArrayList<>();
        List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery);
        
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Book book = new Book();
                
                Object idObj = record.getValueByKey("id");
                if (idObj != null) {
                    book.setId(Long.parseLong(idObj.toString()));
                }
                
                Object title = record.getValueByKey("title");
                if (title != null) book.setTitle(title.toString());
                
                Object author = record.getValueByKey("author");
                if (author != null) book.setAuthor(author.toString());
                
                Object category = record.getValueByKey("category");
                if (category != null) book.setCategory(category.toString());
                
                Object content = record.getValueByKey("content");
                if (content != null) book.setContent(content.toString());
                
                Object viewCount = record.getValueByKey("viewCount");
                if (viewCount != null) book.setViewCount(Long.parseLong(viewCount.toString()));
                
                Object downloadCount = record.getValueByKey("downloadCount");
                if (downloadCount != null) book.setDownloadCount(Long.parseLong(downloadCount.toString()));
                
                books.add(book);
            }
        }
        return books;
    }

    @Override
    public void saveBook(Book book) {
        if (book.getId() == null) {
            book.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        }
        book.setContent(null);

        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
        writeApi.writeMeasurement(bucket, org, WritePrecision.NS, book);
        System.out.println("Lưu thành công vào InfluxDB với ID: " + book.getId());
    }

    @Override
    public Page<Book> searchBooks(String title, String author, String content, int page, int size) {
        int offset = page * size;

        StringBuilder flux = new StringBuilder();
        flux.append(String.format("from(bucket: \"%s\") ", bucket))
                .append("|> range(start: 0) ")
                .append(String.format("|> filter(fn: (r) => r._measurement == \"%s\") ", MEASUREMENT))
                .append("|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") ");

        if (title != null && !title.isBlank()) {
            flux.append(String.format("|> filter(fn: (r) => r.title =~ /(?i)%s/) ", title));
        }
        if (author != null && !author.isBlank()) {
            flux.append(String.format("|> filter(fn: (r) => r.author =~ /(?i)%s/) ", author));
        }

        flux.append(String.format("|> limit(n: %d, offset: %d)", size, offset));

        List<Book> books = mapFluxToBooks(flux.toString());

        return new PageImpl<>(books, PageRequest.of(page, size), books.size());
    }

    @Override
    public void updateBook(Long id, Book book) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: 0) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                        "|> filter(fn: (r) => r.id == \"%d\") " + // Ép ID thành chuỗi để so sánh Tag
                        "|> last()", bucket, MEASUREMENT, id);

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);
        if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
            book.setId(id);
            saveBook(book);
            System.out.println("Cập nhật thành công bản ghi InfluxDB");
        } else {
            System.out.println("Update không thành công: Không tìm thấy ID");
        }
    }

    @Override
    public void deleteBooks(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;

        OffsetDateTime start = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime stop = OffsetDateTime.now();

        String predicate = ids.stream()
                .map(id -> "id = \"" + id + "\"")
                .collect(Collectors.joining(" OR "));

        String finalPredicate = "(_measurement = \"" + MEASUREMENT + "\") AND (" + predicate + ")";

        try {
            influxDBClient.getDeleteApi().delete(start, stop, finalPredicate, bucket, org);
            System.out.println("Đã xóa các sách có ID: " + ids);
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa dữ liệu InfluxDB: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: 0) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> filter(fn: (r) => r._field == \"title\") " +
                        "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                        "|> filter(fn: (r) => r.author == \"%s\") ", bucket, MEASUREMENT, author);

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);

        long totalBooks = 0;
        for (FluxTable table : tables) {
            totalBooks += table.getRecords().size();
        }
        if (totalBooks == 0) {
            return Map.of("message", "Không tìm thấy dữ liệu tác giả: " + author);
        }

        return Map.of(
                "author", author,
                "totalBooks", totalBooks,
                "lastUpdated", Instant.now()
        );
    }

    @Override
    public List<Book> findAllPaging(int page, int size) {
        int offset = page * size;

        String flux = String.format("from(bucket: \"%s\") ", bucket) +
                "|> range(start: 0) " +
                String.format("|> filter(fn: (r) => r._measurement == \"%s\") ", MEASUREMENT) +
                "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                "|> sort(columns: [\"_time\"], desc: true) " +
                String.format("|> limit(n: %d, offset: %d)", size, offset);

        try {
            return mapFluxToBooks(flux);
        } catch (Exception e) {
            System.err.println("Lỗi truy vấn InfluxDB: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}