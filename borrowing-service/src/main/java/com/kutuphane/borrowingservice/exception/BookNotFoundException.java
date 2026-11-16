package com.kutuphane.borrowingservice.exception;

// Book-Service "böyle bir kitap yok" (404) dediğinde fırlatılır -> HTTP 404
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String message) {
        super(message);
    }
}
