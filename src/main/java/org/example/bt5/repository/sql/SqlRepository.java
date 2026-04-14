package org.example.bt5.repository.sql;

import org.example.bt5.model.BookSQL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
            "category, " +
            "SUM(book_count) as total_books, " +
            "SUM(SUM(book_count)) OVER() as total_all " +
            "FROM view_author_stats " +
            "WHERE author = :author " +
            "GROUP BY category",
            nativeQuery = true)
    List<Map<String, Object>> statisticByAuthor(@Param("author") String author);

    void update(Long id, String title, String author, String content, String category, Long viewCount);
}