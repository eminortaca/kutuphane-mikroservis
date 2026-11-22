package com.kutuphane.borrowingservice.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

// Borrowing-Service'teki tüm hataları yakalayıp doğru HTTP koduna çevirir.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Kitap yok -> 404
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(BookNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Ödünç kaydı yok -> 404
    @ExceptionHandler(BorrowingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBorrowingNotFound(BorrowingNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Üye yok -> 404
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Stok bitti -> 409
    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleOutOfStock(OutOfStockException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Kitap zaten iade edilmiş -> 409
    @ExceptionHandler(BookAlreadyReturnedException.class)
    public ResponseEntity<ErrorResponse> handleBookAlreadyReturned(BookAlreadyReturnedException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Ödünç alma / süre uzatma iş kuralı ihlali -> 409
    @ExceptionHandler(BorrowingRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleRuleViolation(BorrowingRuleViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Başka üyenin ödünç kaydına erişim denemesi -> 403
    @ExceptionHandler(BorrowingAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(BorrowingAccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // Eşzamanlı güncelleme çakışması (@Version) -> 409
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockConflict(ObjectOptimisticLockingFailureException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Bu kayıt eşzamanlı bir işlemle güncellendi, lütfen tekrar deneyin.");
    }

    // Geçersiz istek parametresi kombinasyonu -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // İstek gövdesi doğrulama hatası (@Valid) -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    // İstek parametresi doğrulama hatası (@Validated + @RequestParam) -> 400
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    // Zorunlu parametre eksik gönderilirse -> 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Eksik parametre: " + ex.getParameterName());
    }

    // Book-Service'e ulaşılamıyor -> 503
    @ExceptionHandler(BookServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleBookServiceUnavailable(BookServiceUnavailableException ex) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    // Member-Service'e ulaşılamıyor -> 503
    @ExceptionHandler(MemberServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleMemberServiceUnavailable(MemberServiceUnavailableException ex) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    // Kayıt yazılamadı (stok geri verildi) -> 500
    @ExceptionHandler(BorrowingFailedException.class)
    public ResponseEntity<ErrorResponse> handleBorrowingFailed(BorrowingFailedException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
