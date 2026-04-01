package org.example.bt5.controller;


import lombok.RequiredArgsConstructor;
import org.example.bt5.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/books/api")
public class BookController {

    private final BookService<Object> bookService;

    @GetMapping("/{db}")
    public ResponseEntity<Page<?>> getBooks(@PathVariable String db, Pageable pageable) {
        return ResponseEntity.ok(bookService.findAllPaging(db, pageable));
    }

    @PostMapping("/{db}")
    public ResponseEntity<Object> createBook(@PathVariable String db, @RequestBody Object book) {
        bookService.saveBook(book, db);
        return ResponseEntity.ok(book);
    }

    @GetMapping("/{db}/search")
    public ResponseEntity<Page<?>> searchBooks(
            @PathVariable String db,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String content,
            Pageable pageable
    ) {
        return ResponseEntity.ok(bookService.searchBooks(db, title, author, content, pageable));
    }

    @PutMapping("/{db}/{id}")
    public ResponseEntity<String> updateBook(
            @PathVariable String db,
            @PathVariable String id, // Đã khớp String cho UUID
            @RequestBody Object book
    ) {
        bookService.updateBook(db, id, book);
        return ResponseEntity.ok("Update success");
    }

    @DeleteMapping("/{db}")
    public ResponseEntity<String> deleteBooks(
            @PathVariable String db,
            @RequestBody List<String> ids // Đồng bộ List<String> cho UUID
    ) {
        bookService.deleteBooks(db, ids);
        return ResponseEntity.ok("Delete success");
    }

    @GetMapping("/{db}/statistic")
    public ResponseEntity<Map<String, Object>> statisticByAuthor(
            @PathVariable String db,
            @RequestParam String author
    ) {
        return ResponseEntity.ok(bookService.statisticByAuthor(db, author));
    }
}