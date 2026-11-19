package com.kutuphane.bookservice.dto;

import com.kutuphane.bookservice.entity.Book;

// İstemciye dönen kitap verisi.
// Entity'yi (Book) doğrudan dışarı açmıyoruz; böylece veritabanı yapısı değişse bile
// API sözleşmesi bozulmaz ve istemeden iç alanlar (ileride eklenecek audit alanları vb.) sızmaz.
public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String category,
        Integer publishYear,
        Integer stock,
        Integer totalCopies,
        Integer availableCopies
) {
    public static BookResponse from(Book book) {
        Integer total = book.getTotalCopies() != null ? book.getTotalCopies() : book.getStock();
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getCategory(),
                book.getPublishYear(),
                book.getStock(),
                total,
                book.getStock()
        );
    }
}
