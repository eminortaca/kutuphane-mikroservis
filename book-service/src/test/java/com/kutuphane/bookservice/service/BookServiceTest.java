package com.kutuphane.bookservice.service;

import com.kutuphane.bookservice.dto.BookRequest;
import com.kutuphane.bookservice.entity.Book;
import com.kutuphane.bookservice.exception.BookInUseException;
import com.kutuphane.bookservice.exception.DuplicateIsbnException;
import com.kutuphane.bookservice.exception.OutOfStockException;
import com.kutuphane.bookservice.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void createBook_shouldSaveBookAndDefaultTotalCopiesToStock() {
        BookRequest request = new BookRequest("Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 4, null);
        when(bookRepository.existsByIsbnIgnoreCase("978-01")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        Book created = bookService.createBook(request);

        assertEquals(1L, created.getId());
        assertEquals(4, created.getStock());
        assertEquals(4, created.getTotalCopies());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void createBook_whenDuplicateIsbn_shouldThrowDuplicateIsbnException() {
        BookRequest request = new BookRequest("Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 2, 2);
        when(bookRepository.existsByIsbnIgnoreCase("978-01")).thenReturn(true);

        assertThrows(DuplicateIsbnException.class, () -> bookService.createBook(request));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateBook_whenBorrowedCopiesExist_shouldAdjustAvailableStock() {
        Book existing = new Book(1L, "Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 2, 3);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.existsByIsbnIgnoreCaseAndIdNot("978-01", 1L)).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1 kopya ödünçte (3 - 2 = 1). Yeni totalCopies = 5 ise raftaki stok 4 olmalı.
        BookRequest update = new BookRequest("Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 0, 5);
        Book updated = bookService.updateBook(1L, update);

        assertEquals(5, updated.getTotalCopies());
        assertEquals(4, updated.getStock());
    }

    @Test
    void updateBook_whenNewTotalCopiesLessThanBorrowed_shouldThrowBookInUseException() {
        Book existing = new Book(1L, "Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 2, 3);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.existsByIsbnIgnoreCaseAndIdNot("978-01", 1L)).thenReturn(false);

        // 1 kopya ödünçteyken totalCopies = 0 yapılamaz
        BookRequest update = new BookRequest("Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 0, 0);

        assertThrows(BookInUseException.class, () -> bookService.updateBook(1L, update));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void deleteBook_whenBorrowedCopiesExist_shouldThrowBookInUseException() {
        Book existing = new Book(1L, "Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 1, 2);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(BookInUseException.class, () -> bookService.deleteBook(1L));
        verify(bookRepository, never()).delete(any());
    }

    @Test
    void deleteBook_whenNoBorrowedCopies_shouldDeleteBook() {
        Book existing = new Book(1L, "Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 2, 2);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        bookService.deleteBook(1L);

        verify(bookRepository).delete(existing);
    }

    @Test
    void decreaseStock_whenOutOfStock_shouldThrowOutOfStockException() {
        Book existing = new Book(1L, "Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 0, 2);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.decreaseStockIfAvailable(1L)).thenReturn(0);

        assertThrows(OutOfStockException.class, () -> bookService.decreaseStock(1L));
    }
}
