package com.kutuphane.borrowingservice;

// Zaten iade edilmiş bir ödünç kaydı tekrar iade edilmeye çalışılırsa fırlatılır -> HTTP 409 (Conflict)
public class BookAlreadyReturnedException extends RuntimeException {
    public BookAlreadyReturnedException(Long id) {
        super("Bu kitap zaten iade edilmiş! Ödünç ID: " + id);
    }
}
