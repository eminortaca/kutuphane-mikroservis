package com.kutuphane.bookservice.repository;

import com.kutuphane.bookservice.entity.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "eureka.client.enabled=false")
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void decreaseStockIfAvailable_whenStockPositive_shouldDecreaseAndReturnOne() {
        Book saved = bookRepository.save(new Book(null, "Suç ve Ceza", "Dostoyevski", "ISBN-1", "Roman", 1866, 1, 1));

        int updated = bookRepository.decreaseStockIfAvailable(saved.getId());

        assertEquals(1, updated);
        Book reloaded = bookRepository.findById(saved.getId()).orElseThrow();
        assertEquals(0, reloaded.getStock());
    }

    @Test
    void decreaseStockIfAvailable_whenStockZero_shouldNotGoNegativeAndReturnZero() {
        Book saved = bookRepository.save(new Book(null, "Suç ve Ceza", "Dostoyevski", "ISBN-2", "Roman", 1866, 0, 1));

        int updated = bookRepository.decreaseStockIfAvailable(saved.getId());

        assertEquals(0, updated);
        Book reloaded = bookRepository.findById(saved.getId()).orElseThrow();
        assertEquals(0, reloaded.getStock());
    }

    @Test
    void increaseStock_shouldIncrementStockByOne() {
        Book saved = bookRepository.save(new Book(null, "Nutuk", "Atatürk", "ISBN-3", "Tarih", 1927, 1, 2));

        int updated = bookRepository.increaseStock(saved.getId());

        assertEquals(1, updated);
        Book reloaded = bookRepository.findById(saved.getId()).orElseThrow();
        assertEquals(2, reloaded.getStock());
    }

    @Test
    void searchBooks_shouldFilterByTitleAuthorCategoryCaseInsensitiveAndPaginate() {
        bookRepository.save(new Book(null, "Suç ve Ceza", "Fyodor Dostoyevski", "ISBN-4", "Roman", 1866, 3, 3));
        bookRepository.save(new Book(null, "Kürk Mantolu Madonna", "Sabahattin Ali", "ISBN-5", "Roman", 1943, 2, 2));
        bookRepository.save(new Book(null, "Nutuk", "Mustafa Kemal Atatürk", "ISBN-6", "Tarih", 1927, 5, 5));

        Page<Book> result = bookRepository.searchBooks("suç", null, "roman", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Suç ve Ceza", result.getContent().getFirst().getTitle());
    }
}
