package com.kutuphane.bookservice.controller;

import com.kutuphane.bookservice.dto.BookRequest;
import com.kutuphane.bookservice.entity.Book;
import com.kutuphane.bookservice.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // 1. Yeni kitap ekle (DTO + @Valid ile girdi doğrulaması yapılır)
    @PostMapping
    public Book createBook(@Valid @RequestBody BookRequest request) {
        return bookService.createBook(request);
    }

    // 2. Tüm kitapları listele
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    // 3. ID'ye göre kitap getir (Borrowing-Service bunu çağıracak!)
    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookService.getBookById(id);
    }

    // 4. Kitap ödünç alındığında stoğu 1 düşür (Borrowing-Service bunu da çağıracak!)
    @PutMapping("/{id}/decrease-stock")
    public Book decreaseStock(@PathVariable Long id) {
        return bookService.decreaseStock(id);
    }

    // 5. Stoğu 1 artır (İADE veya TELAFİ işlemi)
    // Kitap iade edildiğinde ya da ödünç kaydı yazılamadığında stoğu geri vermek için çağrılır.
    @PutMapping("/{id}/increase-stock")
    public Book increaseStock(@PathVariable Long id) {
        return bookService.increaseStock(id);
    }
}
