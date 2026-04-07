package org.example.bt5.model;

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
@Table(name = "book", indexes = {
        @Index(name = "idx_author_title", columnList = "author, title"),
        @Index(name = "idx_title", columnList = "title")
})

public class BookSQL {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String author;
    private String category;
    private String title;
    private String content;
    private Instant createDate = Instant.now();
    public Long viewCount;
    public Long downloadCount;

}