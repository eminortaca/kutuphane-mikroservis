package com.kutuphane.borrowingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "borrowings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Borrowing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookId;         // Sadece kitabın ID'sini tutuyoruz (Foreign Key değil!)
    private String bookTitle;    // Sorgularda kolaylık olsun diye kitabın adını da kaydedelim
    private String borrowerName; // Ödünç alan kişinin adı
    private LocalDate borrowDate;
    private LocalDate returnDate; // İade edildiği tarih (henüz iade edilmediyse null)

    @Enumerated(EnumType.STRING)
    private BorrowingStatus status; // BORROWED veya RETURNED
}
