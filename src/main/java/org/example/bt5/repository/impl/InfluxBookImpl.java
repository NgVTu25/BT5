package org.example.bt5.repository.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import org.example.bt5.model.BookMetric;
import org.example.bt5.repository.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Repository("Influx")
@RequiredArgsConstructor
public class InfluxBookImpl implements BookRepository<BookMetric, Long> {
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

    private List<BookMetric> mapFluxToBooks(String fluxQuery) {
        List<BookMetric> books = new ArrayList<>();
        List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery);

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                BookMetric book = new BookMetric();

                Object idObj = record.getValueByKey("id");
                if (idObj != null) {
                    try {
                        book.setId((Long) idObj);
                    } catch (NumberFormatException ignore) { }
                }

                Object title = record.getValueByKey("title");
                if (title != null) book.setTitle(title.toString());

                Object author = record.getValueByKey("author");
                if (author != null) book.setAuthor(author.toString());

                Object category = record.getValueByKey("category");
                if (category != null) book.setCategory(category.toString());

                Object viewCount = record.getValueByKey("viewCount");
                if (viewCount != null) {
                    try {
                        book.setViewCount(Long.parseLong(viewCount.toString()));
                    } catch (NumberFormatException ignore) { }
                }

                Object downloadCount = record.getValueByKey("downloadCount");
                if (downloadCount != null) {
                    try {
                        book.setDownloadCount(Long.parseLong(downloadCount.toString()));
                    } catch (NumberFormatException ignore) { }
                }

                books.add(book);
            }
        }
        return books;
    }

    @Override
    public void saveBook(BookMetric book) {
        if (book.getId() == null) {
            Long ID = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
            book.setId(ID);
        }

        influxDBClient.getWriteApiBlocking().writePoint(bucket, org, writeData(book));
        System.out.println("-> Save book with ID: " + book.getId());
    }

    @Override
    public Page<BookMetric> searchBooks(String title, String author, String content, Pageable pageable) {
        int page = pageable == null ? 0 : pageable.getPageNumber();
        int size = pageable == null ? 10 : pageable.getPageSize();
        int offset = page * size;

        StringBuilder flux = new StringBuilder();
        flux.append(String.format("from(bucket: \"%s\") ", bucket))
                .append("|> range(start: 0) ")
                .append(String.format("|> filter(fn: (r) => r._measurement == \"%s\") ", MEASUREMENT));

        if (author != null && !author.isBlank()) {
            flux.append(String.format("|> filter(fn: (r) => r.author =~ /(?i)%s/) ", author));
        }

        flux.append("|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") ");

        if (title != null && !title.isBlank()) {
            flux.append(String.format("|> filter(fn: (r) => r.title =~ /(?i)%s/) ", title));
        }

        flux.append(String.format("|> limit(n: %d, offset: %d)", size, offset));

        List<BookMetric> books = mapFluxToBooks(flux.toString());
        return new PageImpl<>(books, PageRequest.of(page, size), books.size());
    }

    public void saveAll(List<BookMetric> books) {
        if (books == null || books.isEmpty()) return;

        List<Point> points = new ArrayList<>();
        for (BookMetric book : books) {

            if (book.getId() == null) {
                book.setId(Math.abs(UUID.randomUUID().getMostSignificantBits()));
            }

            points.add(writeData(book));
        }

        influxDBClient.getWriteApiBlocking().writePoints(bucket, org, points);
        System.out.println("-> Batch save " + books.size() + " books successful.");
    }

    public Point writeData(BookMetric book) {
        Point point = Point.measurement(MEASUREMENT)
                .addTag("id", String.valueOf(book.getId()))
                .addTag("author", book.getAuthor() == null ? "Unknown" : book.getAuthor())
                .addTag("category", book.getCategory() == null ? "General" : book.getCategory())

                .addField("title", book.getTitle() == null ? "" : book.getTitle())

                .addField("viewCount", book.getViewCount() == null ? 0L : book.getViewCount())
                .addField("downloadCount", book.getDownloadCount() == null ? 0L : book.getDownloadCount())

                .time(book.getCreateDate() != null ? book.getCreateDate() : Instant.now(), WritePrecision.NS);
        return point;
    }


    @Override
    public void updateBook(Long id, BookMetric book) {

        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: 0) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> filter(fn: (r) => r.id == \"%s\") " +
                        "|> limit(n: 1)",
                bucket, MEASUREMENT, id.toString()
        );

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);

        boolean exists = tables.stream().anyMatch(t -> !t.getRecords().isEmpty());
        if (!exists) {
            System.out.println("Update không thành công: Không tìm thấy ID");
            return;
        }

        deleteBooks(List.of(Long.valueOf(id.toString())));
        book.setId(id);
        saveBook(book);
        System.out.println("Cập nhật thành công bản ghi InfluxDB");
    }

    @Override
    public void deleteBooks(List<Long> ids) {
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
                        "|> filter(fn: (r) => r.author == \"%s\") " +
                        "|> filter(fn: (r) => r._field == \"title\") " +
                        "|> group() " +
                        "|> count()",
                bucket, MEASUREMENT, author
        );

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);

        long totalBooks = 0;
        if (!tables.isEmpty() && !tables.getFirst().getRecords().isEmpty()) {
            totalBooks = Long.parseLong(Objects.requireNonNull(tables.getFirst().getRecords().getFirst().getValueByKey("_value")).toString());
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

    public long countTotalBooks() {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: 0) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> filter(fn: (r) => r._field == \"title\") " +
                        "|> group() " +
                        "|> count()",
                bucket, MEASUREMENT
        );
        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);
        if (!tables.isEmpty() && !tables.getFirst().getRecords().isEmpty()) {
            return Long.parseLong(Objects.requireNonNull(tables.getFirst().getRecords().getFirst().getValueByKey("_value")).toString());
        }
        return 0L;
    }

    @Override
    public Page<BookMetric> findAllPaging(Pageable pageable) {

        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: 0) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> filter(fn: (r) => " +
                        "r._field == \"author\" or " +
                        "r._field == \"category\" or " +
                        "r._field == \"title\") " +
                        "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                        "|> keep(columns: [\"_time\", \"author\", \"category\", \"title\"]) " +
                        "|> group() " +
                        "|> sort(columns: [\"_time\"], desc: true) " +
                        "|> limit(n: %d, offset: %d)",
                bucket, MEASUREMENT, pageable.getPageSize(), pageable.getOffset()
        );

        List<BookMetric> books = mapFluxToBooks(flux);
        int totalBooks = (int) countTotalBooks();

        return new PageImpl<>(books, pageable, totalBooks);
    }
}