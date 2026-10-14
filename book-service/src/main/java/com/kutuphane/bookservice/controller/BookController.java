package com.kutuphane.bookservice.controller;

import com.kutuphane.bookservice.entity.Book;
import com.kutuphane.bookservice.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookRepository bookRepository;

    // 1. Yeni kitap ekle
    @PostMapping
    public Book createBook(@RequestBody Book book) {
        return bookRepository.save(book);
    }

    // 2. Tüm kitapları listele
    @GetMapping
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // 3. ID'ye göre kitap getir (Borrowing-Service bunu çağıracak!)
    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kitap bulunamadı! ID: " + id));
    }

    // 4. Kitap ödünç alındığında stoğu 1 düşür (Borrowing-Service bunu da çağıracak!)
    @PutMapping("/{id}/decrease-stock")
    public Book decreaseStock(@PathVariable Long id) {
        Book book = getBookById(id);
        if (book.getStock() <= 0) {
            throw new RuntimeException("Kitap stoğu tükenmiş!");
        }
        book.setStock(book.getStock() - 1);
        return bookRepository.save(book);
    }
}