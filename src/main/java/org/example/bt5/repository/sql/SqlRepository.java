package org.example.bt5.repository.sql;

import org.example.bt5.model.BookSQL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;

public interface SqlRepository extends JpaRepository<BookSQL, Long> {

    @Query("SELECT b FROM BookSQL b WHERE " +
            "(:title IS NULL OR b.title LIKE %:title%) AND " +
            "(:author IS NULL OR b.author LIKE %:author%) AND " +
            "(:content IS NULL OR b.content LIKE %:content%)")
    Page<BookSQL> searchBooksCustom(@Param("title") String title,
                                    @Param("author") String author,
                                    @Param("content") String content,
                                    Pageable pageable);

    @Query("""
                SELECT 
                    COUNT(b) as totalBooks,
                    SUM(b.viewCount) as totalViews,
                    AVG(b.viewCount) as avgViews,
                    MAX(b.viewCount) as maxViews
                FROM BookSQL b
                WHERE b.author = :author
            """)
    Map<String, Object> statisticByAuthor(@Param("author") String author);
}
