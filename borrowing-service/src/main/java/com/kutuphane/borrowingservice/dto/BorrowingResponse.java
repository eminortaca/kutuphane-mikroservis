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
        Long memberId,
        String borrowerName,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        Integer renewCount,
        Double lateFee,
        BorrowingStatus status
) {
    public static BorrowingResponse from(Borrowing borrowing) {
        return new BorrowingResponse(
                borrowing.getId(),
                borrowing.getBookId(),
                borrowing.getBookTitle(),
                borrowing.getMemberId(),
                borrowing.getBorrowerName(),
                borrowing.getBorrowDate(),
                borrowing.getDueDate(),
                borrowing.getReturnDate(),
                borrowing.getRenewCount(),
                borrowing.getLateFee(),
                borrowing.getStatus()
        );
    }
}
