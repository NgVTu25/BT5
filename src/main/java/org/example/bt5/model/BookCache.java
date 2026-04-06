package org.example.bt5.model;

import com.influxdb.annotations.Column;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("book")
public class BookCache {
    @Id
    private String id;
    @Indexed
    private String author;
    @Indexed
    private String category;
    @Indexed
    private String title;

    private String content;

    private Instant createDate = Instant.now();

    public Long viewCount;

    public Long downloadCount;

}