package com.kutuphane.bookservice.dto;

import com.kutuphane.bookservice.entity.Book;

// İstemciye dönen kitap verisi.
// Entity'yi (Book) doğrudan dışarı açmıyoruz; böylece veritabanı yapısı değişse bile
// API sözleşmesi bozulmaz ve istemeden iç alanlar (ileride eklenecek audit alanları vb.) sızmaz.
public record BookResponse(
        Long id,
        String title,
        String author,
        Integer stock
) {
    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getStock());
    }
}
