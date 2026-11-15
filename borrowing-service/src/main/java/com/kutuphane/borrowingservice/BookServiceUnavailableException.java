package com.kutuphane.borrowingservice;

// Book-Service kapalı, çok yavaş veya devre kesici (circuit breaker) açıksa fırlatılır -> HTTP 503
public class BookServiceUnavailableException extends RuntimeException {
    public BookServiceUnavailableException(String message) {
        super(message);
    }
}
