package com.kutuphane.borrowingservice.exception;

// Stok düştükten sonra ödünç kaydı yazılamazsa fırlatılır (stok geri verilmiştir) -> HTTP 500
public class BorrowingFailedException extends RuntimeException {
    public BorrowingFailedException(String message) {
        super(message);
    }
}
