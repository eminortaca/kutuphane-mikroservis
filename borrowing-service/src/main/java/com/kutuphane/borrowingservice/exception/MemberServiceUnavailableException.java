package com.kutuphane.borrowingservice.exception;

// Member-Service kapalı, zaman aşımına uğradı veya devre kesici açık -> HTTP 503
public class MemberServiceUnavailableException extends RuntimeException {
    public MemberServiceUnavailableException(String message) {
        super(message);
    }
}
