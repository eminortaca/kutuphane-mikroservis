package com.kutuphane.bookservice.repository;

import com.kutuphane.bookservice.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbnIgnoreCase(String isbn);

    boolean existsByIsbnIgnoreCaseAndIdNot(String isbn, Long id);

    // Başlık, yazar veya kategoriye göre büyük/küçük harf duyarsız arama + sayfalama
    @Query("""
            SELECT b FROM Book b
            WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')))
              AND (:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%')))
              AND (:category IS NULL OR LOWER(b.category) LIKE LOWER(CONCAT('%', :category, '%')))
            """)
    Page<Book> searchBooks(
            @Param("title") String title,
            @Param("author") String author,
            @Param("category") String category,
            Pageable pageable);

    // STOĞU GÜVENLİ (ATOMİK) ŞEKİLDE 1 DÜŞÜR
    // Eskiden: önce kitabı oku -> stok > 0 mı bak -> stock-1 yap -> kaydet (3 ayrı adım).
    // Aynı anda 2 kişi son kitabı isterse ikisi de "stok 1" görüp ikisi de düşürebiliyordu!
    // Şimdi: kontrol + düşürme TEK bir SQL cümlesinde yapılıyor, veritabanı bunu sıraya sokar.
    // Geriye dönen sayı = güncellenen satır sayısı. 0 ise -> stok yokmuş demektir.
    @Transactional
    @Modifying(clearAutomatically = true) // Güncellemeden sonra eski (bayat) Book nesnesi bellekte kalmasın
    @Query("UPDATE Book b SET b.stock = b.stock - 1 WHERE b.id = :id AND b.stock > 0")
    int decreaseStockIfAvailable(@Param("id") Long id);

    // STOĞU 1 ARTIR (İade veya telafi işlemi için)
    // Borrowing-Service stoğu düşürdükten sonra kendi kaydını yazamazsa, stoğu geri vermek için bunu çağırır.
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Book b SET b.stock = b.stock + 1 WHERE b.id = :id")
    int increaseStock(@Param("id") Long id);
}
