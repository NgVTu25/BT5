package org.example.bt4.repository.mongo;


import org.example.bt4.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Map;

public interface MongoDBRepository extends MongoRepository<Book, Long> {
    Page<Book> findByTitleContainingIgnoreCaseAndAuthorContainingIgnoreCaseAndContentContainingIgnoreCase(String title,
             String author, String content, Pageable pageable);

    @Aggregation(pipeline = {
            "{ '$match': { 'author': ?0 } }",
            "{ '$group': { " +
                    "    '_id': '$author', " +
                    "    'totalBooks': { '$sum': 1 }, " +
                    "    'totalViews': { '$sum': '$viewCount' }, " +
                    "    'avgViews': { '$avg': '$viewCount' }, " +
                    "    'maxViews': { '$max': '$viewCount' } " +
                    "} }"
    })
    Map<String, Object> statisticByAuthor(String author);


    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable  pageable);
}
