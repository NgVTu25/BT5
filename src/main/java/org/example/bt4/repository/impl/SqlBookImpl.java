package org.example.bt4.repository.impl;

import org.example.bt4.model.Book;
import org.example.bt4.repository.BookRepository;
import org.example.bt4.repository.sql.SqlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository("MySql")
public class SqlBookImpl implements BookRepository {
    private final SqlRepository sqlRepository;

    public SqlBookImpl(SqlRepository sqlRepository) {
        this.sqlRepository = sqlRepository;
    }

    @Override
    public void saveBook(Book book) {
        sqlRepository.save(book);
        System.out.println(book.getId());
    }

    @Override
    public Page<Book> searchBooks(String title, String author, String content , int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("author").ascending());
        return sqlRepository.findByTitleContainingIgnoreCaseAndAuthorContainingIgnoreCaseAndContentContainingIgnoreCase(
                (title == null || title.isEmpty()) ? null : title,
                (author == null || author.isEmpty()) ? null : author,
                (content == null || content.isEmpty()) ? null : content, pageable);
    }

    @Override
    public void updateBook(Long id, Book book) {
        Book upBook = sqlRepository.findById(id).orElse(null);

        if (upBook != null) {
            upBook.setTitle(book.getTitle());
            upBook.setAuthor(book.getAuthor());
            upBook.setContent(book.getContent());
            upBook.setCategory(book.getCategory());
            upBook.setViewCount(book.getViewCount());

            sqlRepository.save(upBook);
        } else {
            System.err.println("[LỖI] Không tìm thấy sách với ID: " + id);
        }
    }

    @Override
    public void deleteBooks(List<String> ids) {
        for(String id : ids) {
            Long BookID = Long.parseLong(id);
            sqlRepository.deleteById(BookID);
        }
    }

    @Override
    public Map<String, Object> statisticByAuthor(String author) {
        return  sqlRepository.statisticByAuthor(author);
    }

    @Override
    public List<Book> findAllPaging(int page, int size) {
        return sqlRepository.findAll(PageRequest.of(page, size)).getContent();
    }
}
