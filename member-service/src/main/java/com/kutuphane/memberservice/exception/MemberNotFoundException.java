package com.kutuphane.memberservice.exception;

// Aranan ID'de üye yoksa fırlatılır -> HTTP 404 (Not Found)
public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(Long id) {
        super("Üye bulunamadı! ID: " + id);
    }
}
