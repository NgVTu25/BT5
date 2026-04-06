package org.example.bt5;

import org.example.bt5.model.BookCache;
import org.example.bt5.repository.BookRepository;
import org.example.bt5.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private Map<String, BookRepository> bookRepositories;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BookRepository bookRepositoryMock;

    @InjectMocks
    private BookService<Object> bookService;

    @Test
    void saveBook() {
        Object inputBook = new Object();
        BookCache convertedBook = new BookCache();
        convertedBook.setTitle("Test Book");

        when(bookRepositories.get("redis")).thenReturn(bookRepositoryMock);
        when(objectMapper.convertValue(any(), eq(BookCache.class))).thenReturn(convertedBook);

        bookService.saveBook(inputBook, "redis");

        verify(bookRepositoryMock, times(1)).saveBook(convertedBook);
    }

    @Test
    void saveBook_WithInvalidDb() {
        when(bookRepositories.get("invalid_db")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            bookService.saveBook(new Object(), "invalid_db");
        });
    }
}