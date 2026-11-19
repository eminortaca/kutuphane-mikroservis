package com.kutuphane.bookservice.controller;

import com.kutuphane.bookservice.dto.BookRequest;
import com.kutuphane.bookservice.dto.BookResponse;
import com.kutuphane.bookservice.entity.Book;
import com.kutuphane.bookservice.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    // 2. Kitapları listele / ara / sayfala (X-Total-Count ve X-Total-Pages başlıklarıyla)
    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Page<Book> result = bookService.searchBooks(title, author, category, page, size);
        List<BookResponse> body = result.getContent().stream()
                .map(BookResponse::from)
                .toList();
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(result.getTotalPages()))
                .body(body);
    }

    // 3. ID'ye göre kitap getir
    @GetMapping("/{id}")
    public BookResponse getBookById(@PathVariable Long id) {
        return BookResponse.from(bookService.getBookById(id));
    }

    // 4. Kitap bilgilerini ve kopya sayısını güncelle
    @PutMapping("/{id}")
    public BookResponse updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return BookResponse.from(bookService.updateBook(id, request));
    }

    // 5. Kitap sil (ödünçte kopyası yoksa -> 204 No Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
