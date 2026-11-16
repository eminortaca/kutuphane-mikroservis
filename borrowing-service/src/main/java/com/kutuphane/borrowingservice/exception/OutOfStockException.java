package com.kutuphane.borrowingservice.exception;

// Book-Service "stok kalmadı" (409) dediğinde fırlatılır -> HTTP 409
public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) {
        super(message);
    }
}
