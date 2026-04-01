package org.example.bt5.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.bt5.model.BookDocument;
import org.example.bt5.repository.BookRepository;
import org.example.bt5.repository.mongo.MongoDBRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository("Mongo")
@RequiredArgsConstructor
public class MongoBookImpl implements BookRepository<BookDocument, String> {
    private final MongoDBRepository mongoDBRepository;

    private final MongoTemplate mongoTemplate;


    @Override
    public void saveBook(BookDocument book) {
        mongoDBRepository.save(book);
        System.out.println("Lưu thành công vào Mongo với ID: " + book.getId());
    }

    @Override
    public Page<BookDocument> searchBooks(String title, String author, String content, Pageable pageable) {
        Pageable finalPageable = (pageable != null) ? pageable : PageRequest.of(0, 10, Sort.by("title").ascending());

        return mongoDBRepository.findByTitleContainingIgnoreCaseAndAuthorContainingIgnoreCaseAndContentContainingIgnoreCase(
                title != null ? title.trim() : "",
                author != null ? author.trim() : "",
                content != null ? content.trim() : "",
                finalPageable
        );
    }

    @Override
    public void updateBook(String id, BookDocument book) {
        BookDocument Ebook = mongoDBRepository.findById(id).orElse(null);
        if (Ebook != null) {
            Ebook.setTitle(book.getTitle());
            Ebook.setAuthor(book.getAuthor());
            Ebook.setContent(book.getContent());
            Ebook.setCategory(book.getCategory());
            Ebook.setViewCount(book.getViewCount());
            mongoDBRepository.save(Ebook);
        }
    }

    @Override
    public void deleteBooks(List<String> ids) {
        for (String id : ids) {
            mongoDBRepository.deleteById(id);
        }
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {
        return mongoDBRepository.statisticByAuthor(author);
    }

    @Override
    public Page<BookDocument> findAllPaging(Pageable pageable) {

        Pageable finalPageable = (pageable != null) ? pageable : PageRequest.of(0, 10, Sort.by("title").ascending());

        return mongoDBRepository.findAll(finalPageable);
    }
}
