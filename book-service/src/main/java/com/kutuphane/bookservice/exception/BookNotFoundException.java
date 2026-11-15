package com.kutuphane.bookservice.exception;

// Aranan ID'de kitap yoksa fırlatılır -> HTTP 404 (Not Found)
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(Long id) {
        super("Kitap bulunamadı! ID: " + id);
    }
}
