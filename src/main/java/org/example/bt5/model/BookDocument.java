package org.example.bt5.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "books")
@CompoundIndex(name = "auth_cat_idx", def = "{'author': 1, 'category': -1}")
public class BookDocument {
    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String author;

    private String category;

    private String title;

    private String content;

    private Instant createDate = Instant.now();

    public Long viewCount;

    public Long downloadCount;

}