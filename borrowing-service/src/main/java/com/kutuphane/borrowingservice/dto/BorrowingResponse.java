package com.kutuphane.borrowingservice.dto;

import com.kutuphane.borrowingservice.entity.Borrowing;
import com.kutuphane.borrowingservice.entity.BorrowingStatus;

import java.time.LocalDate;

// İstemciye dönen ödünç kaydı verisi.
// Entity'yi (Borrowing) doğrudan dışarı açmıyoruz; veritabanı yapısı ile API sözleşmesi birbirinden bağımsız kalır.
public record BorrowingResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String borrowerName,
        LocalDate borrowDate,
        LocalDate returnDate,
        BorrowingStatus status
) {
    public static BorrowingResponse from(Borrowing borrowing) {
        return new BorrowingResponse(
                borrowing.getId(),
                borrowing.getBookId(),
                borrowing.getBookTitle(),
                borrowing.getBorrowerName(),
                borrowing.getBorrowDate(),
                borrowing.getReturnDate(),
                borrowing.getStatus()
        );
    }
}
