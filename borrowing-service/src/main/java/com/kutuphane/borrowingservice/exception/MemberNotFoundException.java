package com.kutuphane.borrowingservice.exception;

// Member-Service "böyle bir üye yok" (404) dediğinde fırlatılır -> HTTP 404
public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(String message) {
        super(message);
    }
}
