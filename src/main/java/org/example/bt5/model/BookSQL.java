package org.example.bt5.model;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.RedisHash;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "book")
public class BookSQL {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(tag = true)
    private String author;
    @Column(tag = true)
    private String category;
    @Column
    private String title;
    @Column
    private String content;
    @Column(timestamp = true)
    private Instant createDate = Instant.now();
    @Column
    public Long viewCount;
    @Column
    public Long downloadCount;

}