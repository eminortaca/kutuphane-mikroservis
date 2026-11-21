package com.kutuphane.bookservice.exception;

// Servisler arası gizli anahtar (X-Internal-Secret) eksik veya hatalıysa fırlatılır -> HTTP 403 (Forbidden)
public class InternalAccessDeniedException extends RuntimeException {
    public InternalAccessDeniedException(String message) {
        super(message);
    }
}
