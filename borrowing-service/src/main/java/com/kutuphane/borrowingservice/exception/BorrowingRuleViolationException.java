package com.kutuphane.borrowingservice.exception;

// Ödünç alma / uzatma iş kuralları ihlal edildiğinde fırlatılır -> HTTP 409 (Conflict)
public class BorrowingRuleViolationException extends RuntimeException {
    public BorrowingRuleViolationException(String message) {
        super(message);
    }
}
