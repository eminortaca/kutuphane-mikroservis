package com.kutuphane.memberservice.exception;

// Aynı e-posta adresiyle ikinci bir üye oluşturulmaya/güncellenmeye çalışılırsa fırlatılır -> HTTP 409 (Conflict)
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("Bu e-posta adresi ile kayıtlı bir üye zaten mevcut: " + email);
    }
}
