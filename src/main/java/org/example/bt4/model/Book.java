package org.example.bt4.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("Book")
@Document(collection = "book")
@Measurement(name = "Book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(tag = true)
    private Long id;
    @Indexed
    @Column
    private String author;
    @Column
    private String category;
    @Indexed
    private String title;
    @Indexed
    private String content;
    @Column
    private LocalDateTime createDate;
    @Column
    private Long viewCount;
    @Column
    private Long downloadCount;
}
