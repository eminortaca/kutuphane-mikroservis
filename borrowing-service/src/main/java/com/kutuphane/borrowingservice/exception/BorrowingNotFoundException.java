package com.kutuphane.borrowingservice.exception;

// Aranan ID'de ödünç alma kaydı yoksa fırlatılır -> HTTP 404 (Not Found)
public class BorrowingNotFoundException extends RuntimeException {
    public BorrowingNotFoundException(Long id) {
        super("Ödünç alma kaydı bulunamadı! ID: " + id);
    }
}
