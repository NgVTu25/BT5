package org.example.bt4.repository.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Repository("Influx")
public class InfluxBookImpl implements BookRepository {
    public final InfluxDBClient influxDBClient;

    @Value("${influx.url}")
    private  String url;
    @Value("${influx.token}")
    private  String token;
    @Value("${influx.org}")
    private  String org;
    @Value("${influx.bucket}")
    private  String bucket;

    public InfluxBookImpl(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    @Override
    public void saveBook(Book book) {
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
        writeApi.writeMeasurement(bucket, org, WritePrecision.NS, book);
    }


    @Override
    public Page<Book> searchBooks(String title, String author, String content, int page, int size) {
        int offset = page * size;

        StringBuilder flux = new StringBuilder();
        flux.append("from(bucket: \"your-bucket\") ")
                .append("|> range(start: -1y) ")
                .append("|> filter(fn: (r) => r._measurement == \"books\") ");

        if (title != null && !title.isEmpty()) {
            flux.append(String.format("|> filter(fn: (r) => r.name =~ /(?i)%s/) ", title));
        }

        if (author != null && !author.isEmpty()) {
            flux.append(String.format("|> filter(fn: (r) => r.author == \"%s\") ", author));
        }

        if (content != null && !content.isEmpty()) {
            flux.append(String.format("|> filter(fn: (r) => r._field == \"content\" and r._value =~ /(?i)%s/) ", content));
        }

        flux.append("|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") ")
                .append(String.format("|> skip(n: %d) ", offset))
                .append(String.format("|> limit(n: %d)", size));

        List<Book> books = influxDBClient.getQueryApi().query(flux.toString(), Book.class);

        return new PageImpl<>(books, PageRequest.of(page, size), books.size());
    }

    @Override
    public void updateBook(Long id, Book book) {
        String flux = String.format(
                "from(bucket: \"my-bucket\") " +
                        "|> range(start: -1y) " +
                        "|> filter(fn: (r) => r._measurement == \"books\" and r.book_id == \"%s\") " +
                        "|> last()", id);
        book.setContent(null);
        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);
        if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
            Instant timeTable = tables.get(0).getRecords().get(0).getTime();

            Point point = Point.measurement("books")
                    .addTag("id", id.toString())
                    .addField("author", book.getAuthor())
                    .addField("category", book.getCategory())
                    .addField("title", book.getTitle())
                    .addField("content", book.getContent())
                    .addField("viewCount",  book.getViewCount())
                    .addField("downloadCount",  book.getDownloadCount());
            influxDBClient.getWriteApiBlocking().writePoint(point);
        } else {
            System.out.println("Update không thành công");
        }

    }

    @Override
    public void deleteBooks(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;

        OffsetDateTime start = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime stop = OffsetDateTime.now();

        String predicate = ids.stream()
                .map(id -> "book_id = \"" + id + "\"")
                .collect(Collectors.joining(" OR "));

        String finalPredicate = "(_measurement = \"books\") AND (" + predicate + ")";

        try {
            influxDBClient.getDeleteApi().delete(
                    start,
                    stop,
                    finalPredicate,
                    bucket,
                    org
            );
            System.out.println("Đã xóa các sách có ID: " + ids);
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa dữ liệu: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {
        String flux = String.format(
                "from(bucket: \"your-bucket\") " +
                        "|> range(start: -10y) " + // Thống kê trong khoảng thời gian dài
                        "|> filter(fn: (r) => r._measurement == \"books\" and r.author == \"%s\") " +
                        "|> filter(fn: (r) => r._field == \"price\") " +
                        "|> group(columns: [\"author\"]) " +
                        "|> aggregateWindow(every: 1y, fn: count, createEmpty: false)", author);

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);

        long totalBooks = 0;
        for (FluxTable table : tables) {
            totalBooks += table.getRecords().size();
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

        String flux = "from(bucket: \"your-bucket\") " +
                "|> range(start: -30d) " + // Mặc định lấy dữ liệu trong 30 ngày qua
                "|> filter(fn: (r) => r._measurement == \"books\") " +
                "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                "|> sort(columns: [\"_time\"], desc: true) " + // Sắp xếp mới nhất lên đầu
                String.format("|> skip(n: %d) ", offset) +
                String.format("|> limit(n: %d)", size);

        return influxDBClient.getQueryApi().query(flux, Book.class);
    }


}
