package com.kutuphane.borrowingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "borrowings")
@Getter @Setter
@NoArgsConstructor
public class Borrowing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookId;         // Sadece kitabın ID'sini tutuyoruz (Foreign Key değil!)
    private String bookTitle;    // Sorgularda kolaylık olsun diye kitabın adını da kaydedelim
    private Long memberId;       // Member-Service'teki üye ID'si (varsa)
    private String borrowerName; // Ödünç alan kişinin adı
    private LocalDate borrowDate;
    private LocalDate dueDate;   // Son teslim tarihi (varsayılan: borrowDate + 14 gün)
    private LocalDate returnDate; // İade edildiği tarih (henüz iade edilmediyse null)
    private Integer renewCount;  // Süre uzatma sayısı (en fazla 1 kez)
    private Double lateFee;      // Gecikme cezası (TL)

    @Enumerated(EnumType.STRING)
    private BorrowingStatus status; // BORROWED, OVERDUE veya RETURNED

    @Version
    private Long version; // Aynı kaydın aynı anda iki istek tarafından ezilmesini önler (Optimistic Locking)

    public Borrowing(Long id, Long bookId, String bookTitle, Long memberId, String borrowerName,
                     LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate,
                     Integer renewCount, Double lateFee, BorrowingStatus status) {
        this.id = id;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.memberId = memberId;
        this.borrowerName = borrowerName;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.renewCount = renewCount;
        this.lateFee = lateFee;
        this.status = status;
    }
}
