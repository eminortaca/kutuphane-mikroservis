package com.kutuphane.bookservice.controller;

import com.kutuphane.bookservice.dto.BookResponse;
import com.kutuphane.bookservice.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// SERVİSLER ARASI (internal) uç noktalar - SADECE Borrowing-Service çağırmalı!
// API Gateway'de /internal/** için bir rota YOKTUR; yani dış dünyadan (8080 üzerinden) bu adreslere
// ulaşılamaz. Eskiden stok uç noktaları /api/books altındaydı ve herkes stoğu değiştirebiliyordu.
// Not: Servisin kendi portuna (8081) doğrudan erişim, Faz 4'te (güvenlik) kapatılacak.
@RestController
@RequestMapping("/internal/books")
@RequiredArgsConstructor
public class InternalBookController {

    private final BookService bookService;

    // Kitap ödünç alındığında stoğu 1 düşür (stok yoksa 409)
    @PutMapping("/{id}/decrease-stock")
    public BookResponse decreaseStock(@PathVariable Long id) {
        return BookResponse.from(bookService.decreaseStock(id));
    }

    // Stoğu 1 artır (İADE veya TELAFİ işlemi)
    @PutMapping("/{id}/increase-stock")
    public BookResponse increaseStock(@PathVariable Long id) {
        return BookResponse.from(bookService.increaseStock(id));
    }
}
