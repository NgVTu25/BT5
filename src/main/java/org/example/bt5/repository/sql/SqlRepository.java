package org.example.bt5.repository.sql;

import org.example.bt5.model.BookSQL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;

public interface SqlRepository extends JpaRepository<BookSQL, Long>, JpaSpecificationExecutor<BookSQL> {

//    @Query("SELECT b FROM BookSQL b WHERE " +
//            "(:title IS NULL OR :title = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
//            "(:author IS NULL OR :author = '' OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
//            "(:content IS NULL OR :content = '' OR LOWER(b.content) LIKE LOWER(CONCAT('%', :content, '%')))")
//    Page<BookSQL> searchBooksCustom(@Param("title") String title,
//                                    @Param("author") String author,
//                                    @Param("content") String content,
//                                    Pageable pageable);

    @Query(value = """
            SELECT * FROM book
            WHERE MATCH(title, author, content)
            AGAINST (:keyword IN NATURAL LANGUAGE MODE)
            """,
            countQuery = """
                    SELECT COUNT(*) FROM book
                    WHERE MATCH(title, author, content)
                    AGAINST (:keyword IN NATURAL LANGUAGE MODE)
                    """,
            nativeQuery = true)
    Page<BookSQL> searchFullText(@Param("keyword") String keyword, Pageable pageable);

//    @Query("""
//                SELECT
//                    COUNT(b) as totalBooks,
//                    SUM(b.viewCount) as totalViews,
//                    MAX(b.viewCount) as maxViews
//                FROM BookSQL b
//                WHERE b.author = :author
//            """)

    @Query(value = "SELECT " +
            "author, " +
            "GROUP_CONCAT(category SEPARATOR ', ') as categories, " +
            "SUM(book_count) as total_books " +
            "FROM view_author_stats " +
            "WHERE author = :author " +
            "GROUP BY author", nativeQuery = true)
    Map<String, Object> statisticByAuthor(@Param("author") String author);
}