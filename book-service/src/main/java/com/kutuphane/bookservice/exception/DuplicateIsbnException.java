package com.kutuphane.bookservice.exception;

// Aynı ISBN numarasıyla ikinci bir kitap eklenmeye/güncellenmeye çalışıldığında fırlatılır -> HTTP 409 (Conflict)
public class DuplicateIsbnException extends RuntimeException {
    public DuplicateIsbnException(String isbn) {
        super("Bu ISBN numarası ile kayıtlı bir kitap zaten mevcut: " + isbn);
    }
}
