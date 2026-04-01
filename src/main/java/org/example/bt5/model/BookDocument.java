package org.example.bt5.model;

import com.influxdb.annotations.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "books")
public class BookDocument {
    @Id
    @Builder.Default
    private String id  = UUID.randomUUID().toString();
    @Column
    private String author;
    @Column
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