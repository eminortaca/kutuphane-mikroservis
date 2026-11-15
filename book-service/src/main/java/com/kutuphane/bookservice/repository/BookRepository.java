package com.kutuphane.bookservice.repository;

import com.kutuphane.bookservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookRepository extends JpaRepository<Book, Long> {

    // STOĞU GÜVENLİ (ATOMİK) ŞEKİLDE 1 DÜŞÜR
    // Eskiden: önce kitabı oku -> stok > 0 mı bak -> stock-1 yap -> kaydet (3 ayrı adım).
    // Aynı anda 2 kişi son kitabı isterse ikisi de "stok 1" görüp ikisi de düşürebiliyordu!
    // Şimdi: kontrol + düşürme TEK bir SQL cümlesinde yapılıyor, veritabanı bunu sıraya sokar.
    // Geriye dönen sayı = güncellenen satır sayısı. 0 ise -> stok yokmuş demektir.
    @Transactional
    @Modifying(clearAutomatically = true) // Güncellemeden sonra eski (bayat) Book nesnesi bellekte kalmasın
    @Query("UPDATE Book b SET b.stock = b.stock - 1 WHERE b.id = :id AND b.stock > 0")
    int decreaseStockIfAvailable(@Param("id") Long id);

    // STOĞU 1 ARTIR (Telafi / geri alma işlemi için)
    // Borrowing-Service stoğu düşürdükten sonra kendi kaydını yazamazsa, stoğu geri vermek için bunu çağırır.
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Book b SET b.stock = b.stock + 1 WHERE b.id = :id")
    int increaseStock(@Param("id") Long id);
}
