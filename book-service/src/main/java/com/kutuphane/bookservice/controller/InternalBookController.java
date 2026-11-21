package com.kutuphane.bookservice.controller;

import com.kutuphane.bookservice.dto.BookResponse;
import com.kutuphane.bookservice.exception.InternalAccessDeniedException;
import com.kutuphane.bookservice.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// SERVİSLER ARASI (internal) uç noktalar - SADECE Borrowing-Service çağırmalı!
// API Gateway'de /internal/** için rota yoktur; ayrıca servisin kendi portuna (8081)
// doğrudan istek atılsa bile geçerli X-Internal-Secret başlığı yoksa 403 Forbidden döner.
@RestController
@RequestMapping("/internal/books")
@RequiredArgsConstructor
public class InternalBookController {

    private final BookService bookService;

    @Value("${internal.api.secret:kutuphane-internal-secret-2026}")
    private String internalSecret;

    // Kitap ödünç alındığında stoğu 1 düşür (stok yoksa 409)
    @PutMapping("/{id}/decrease-stock")
    public BookResponse decreaseStock(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        validateInternalSecret(secret);
        return BookResponse.from(bookService.decreaseStock(id));
    }

    // Stoğu 1 artır (İADE veya TELAFİ işlemi)
    @PutMapping("/{id}/increase-stock")
    public BookResponse increaseStock(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        validateInternalSecret(secret);
        return BookResponse.from(bookService.increaseStock(id));
    }

    private void validateInternalSecret(String secret) {
        if (secret == null || !MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                internalSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new InternalAccessDeniedException("Bu iç uç noktaya erişim yetkiniz yok!");
        }
    }
}
