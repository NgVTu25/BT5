package org.example.bt4.repository.impl;

import org.example.bt4.model.Book;
import org.example.bt4.repository.BookRepository;
import org.example.bt4.repository.MongoDBRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository("Mongo")
public class MongoBookImpl implements BookRepository {
    private final MongoDBRepository mongoDBRepository;

    public MongoBookImpl(MongoDBRepository mongoDBRepository) {
        this.mongoDBRepository = mongoDBRepository;
    }

    @Override
    public void saveBook(Book book) {
        mongoDBRepository.save(book);
        System.out.println(book.getId());
    }

    @Override
    public Page<Book> searchBooks(String title, String author, String content, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
        return mongoDBRepository.findByNameContainingIgnoreCaseAndAuthorContainingIgnoreCaseAndContentContainingIgnoreCase(
                (title == null || title.isEmpty()) ? null : title,
                (author == null || author.isEmpty()) ? null : author,
                (content == null || content.isEmpty()) ? null : content, pageable);
    }

    @Override
    public void updateBook(Long id, Book book) {
        Book Ubook = mongoDBRepository.findById(id).orElse(null);
        if (Ubook != null) {
            Ubook.setTitle(book.getTitle());
            Ubook.setAuthor(book.getAuthor());
            Ubook.setContent(book.getContent());
            Ubook.setCategory(book.getCategory());
            Ubook.setViewCount(book.getViewCount());
            mongoDBRepository.save(Ubook);
        }
    }

    @Override
    public void deleteBooks(List<String> ids) {
        for (String id : ids) {
            mongoDBRepository.deleteById(Long.valueOf(id));
        }
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {
        return mongoDBRepository.statisticByAuthor(author);
    }

    @Override
    public List<Book> findAllPaging(int page, int size) {
        return mongoDBRepository.findAll(PageRequest.of(page, size)).getContent();
    }
}
