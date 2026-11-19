package com.kutuphane.bookservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "books")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;

    @Column(unique = true)
    private String isbn;

    private String category;
    private Integer publishYear;

    private Integer stock;       // Şu an rafta bulunan mevcut (ödünç verilebilir) kopya sayısı
    private Integer totalCopies; // Kütüphanenin sahip olduğu toplam kopya sayısı
}
