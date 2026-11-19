package com.kutuphane.bookservice.exception;

// Aktif ödünçte kopyası bulunan kitap silinmeye çalışıldığında veya toplam kopya sayısı
// ödünçteki kopya sayısının altına düşürülmeye çalışıldığında fırlatılır -> HTTP 409 (Conflict)
public class BookInUseException extends RuntimeException {
    public BookInUseException(String message) {
        super(message);
    }
}
