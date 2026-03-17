package org.example.bt4.repository;

import org.example.bt4.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;

public interface SqlRepository extends JpaRepository<Book, Long> {

    Page<Book> findByNameContainingIgnoreCaseAndAuthorContainingIgnoreCaseAndContentContainingIgnoreCase(String title, Pageable pageable);

    @Query("""
        SELECT 
            COUNT(b) as totalBooks,
            SUM(b.viewCount) as totalViews,
            AVG(b.viewCount) as avgViews,
            MAX(b.viewCount) as maxViews
        FROM Book b
        WHERE b.author = :author
    """)
    Map<String, Object> statisticByAuthor(@Param("author") String author);

    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
