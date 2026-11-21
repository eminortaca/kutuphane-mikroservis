package com.kutuphane.bookservice.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

// Tüm controller'lardan fırlayan hataları tek bir yerde yakalar ve doğru HTTP koduna çevirir.
// Eskiden her hata RuntimeException olduğu için hep 500 dönüyordu.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Kitap yok -> 404
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(BookNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    // Stok bitti -> 409
    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleOutOfStock(OutOfStockException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Aynı ISBN zaten mevcut -> 409
    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateIsbn(DuplicateIsbnException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Kitap ödünçte kullanılıyor (silinemez / toplam kopya düşürülemez) -> 409
    @ExceptionHandler(BookInUseException.class)
    public ResponseEntity<ErrorResponse> handleBookInUse(BookInUseException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // İç servis gizli anahtarı eksik/hatalı -> 403
    @ExceptionHandler(InternalAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleInternalAccessDenied(InternalAccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // Girdi doğrulama hatası (@Valid) -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
