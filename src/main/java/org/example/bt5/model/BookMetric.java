package org.example.bt5.model;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
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
    @Column(tag = true)
    private String id;
    @Column(tag = true)
    private String author;
    @Column(tag = true)
    private String category;
    @Column
    private String title;
    @Transient
    private String content;
    @Column(timestamp = true)
    private Instant createDate = Instant.now();
    @Column
    public Long viewCount;
    @Column
    public Long downloadCount;

}