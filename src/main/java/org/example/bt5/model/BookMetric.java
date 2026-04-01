package org.example.bt5.model;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Measurement(name = "Book")
public class BookMetric {
    @Id
    private Long id;
    @Column(tag = true)
    private String author;
    @Column(tag = true)
    private String category;
    @Column
    private String title;
    @Column(timestamp = true)
    private Instant createDate = Instant.now();
    @Column
    public Long viewCount;
    @Column
    public Long downloadCount;

}