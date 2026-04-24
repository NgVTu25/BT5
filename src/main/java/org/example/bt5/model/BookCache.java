package org.example.bt5.model;

import com.influxdb.annotations.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("book")
public class BookCache {
    @Column
    public Long viewCount;
    @Column
    public Long downloadCount;
    @Column(tag = true, name = "id")
    @Id
    private String id;
    @Column(tag = true)
    private String author;
    @Column(tag = true)
    private String category;
    @Column
    private String title;
    @Column
    private String content;
    @jakarta.persistence.Column(name = "create_date")
    private Instant createDate = Instant.now();

}