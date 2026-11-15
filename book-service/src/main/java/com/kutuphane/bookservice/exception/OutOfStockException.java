package com.kutuphane.bookservice.exception;

// Kitap var ama stoğu bitmişse fırlatılır -> HTTP 409 (Conflict)
public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String bookTitle) {
        super("Üzgünüz, '" + bookTitle + "' kitabının stoğu kalmamış!");
    }
}
