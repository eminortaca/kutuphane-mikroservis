package com.kutuphane.bookservice.controller;

import com.kutuphane.bookservice.dto.BookRequest;
import com.kutuphane.bookservice.dto.BookResponse;
import com.kutuphane.bookservice.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

// DIŞA AÇIK (public) kitap uç noktaları. API Gateway yalnızca /api/books/** yolunu yönlendirir.
// Stok değiştiren uç noktalar burada DEĞİL, InternalBookController'da (/internal/books) durur.
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // 1. Yeni kitap ekle -> 201 Created + Location: /api/books/{id}
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        BookResponse created = BookResponse.from(bookService.createBook(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // 2. Tüm kitapları listele
    @GetMapping
    public List<BookResponse> getAllBooks() {
        return bookService.getAllBooks().stream()
                .map(BookResponse::from)
                .toList();
    }

    // 3. ID'ye göre kitap getir
    @GetMapping("/{id}")
    public BookResponse getBookById(@PathVariable Long id) {
        return BookResponse.from(bookService.getBookById(id));
    }
}
