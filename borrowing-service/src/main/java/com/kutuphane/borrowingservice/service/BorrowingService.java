package com.kutuphane.borrowingservice.service;

import com.kutuphane.borrowingservice.client.BookClient;
import com.kutuphane.borrowingservice.dto.BookDto;
import com.kutuphane.borrowingservice.entity.Borrowing;
import com.kutuphane.borrowingservice.entity.BorrowingStatus;
import com.kutuphane.borrowingservice.exception.BookAlreadyReturnedException;
import com.kutuphane.borrowingservice.exception.BorrowingFailedException;
import com.kutuphane.borrowingservice.exception.BorrowingNotFoundException;
import com.kutuphane.borrowingservice.repository.BorrowingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

// Ödünç alma ve iade etme iş mantığı burada toplanır; Controller sadece HTTP isteklerini karşılar.
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowingService {

    private final BorrowingRepository borrowingRepository;
    private final BookClient bookClient; // Feign Client'ı enjekte ettik!

    // Kitap ödünç alma işlemi
    public Borrowing borrowBook(Long bookId, String borrowerName) {

        // 1. ADIM: Book-Service'e git ve kitabın stoğunu 1 düşür!
        // Bu tek çağrı hem "kitap var mı" hem "stok var mı" kontrolünü yapar ve güncel kitabı döner:
        //   Kitap yoksa       -> BookNotFoundException (404)
        //   Stok yoksa        -> OutOfStockException (409)  (kontrol Book-Service'te atomik yapılır)
        //   Servis kapalıysa  -> BookServiceUnavailableException (503)
        // Eskiden önce ayrıca "kitabı getir" çağrısı yapıyorduk; gereksiz bir ağ çağrısıydı.
        BookDto book = bookClient.decreaseStock(bookId);

        // 2. ADIM: Kendi veritabanımıza (8082 - borrowingdb) ödünç alma kaydını yaz!
        Borrowing borrowing = new Borrowing();
        borrowing.setBookId(book.getId());
        borrowing.setBookTitle(book.getTitle());
        borrowing.setBorrowerName(borrowerName);
        borrowing.setBorrowDate(LocalDate.now());
        borrowing.setReturnDate(null);
        borrowing.setStatus(BorrowingStatus.BORROWED);

        try {
            return borrowingRepository.save(borrowing);
        } catch (Exception ex) {
            // 3. ADIM (TELAFİ): Stok düştü ama kayıt yazılamadı!
            // İki servis ayrı veritabanı kullandığı için tek bir @Transactional ile geri alamayız.
            // Bu yüzden Book-Service'e "stoğu geri ver" diyoruz (Saga desenindeki telafi adımı).
            log.error("Ödünç kaydı yazılamadı, stok geri veriliyor. bookId={}", bookId, ex);
            compensateIncreaseStock(bookId);
            throw new BorrowingFailedException("Ödünç alma kaydı oluşturulamadı, işlem geri alındı.");
        }
    }

    // Kitap iade etme işlemi
    public Borrowing returnBook(Long borrowingId) {

        // 1. ADIM: Kendi veritabanımızda ödünç kaydını bul (yoksa 404)
        Borrowing borrowing = getBorrowingById(borrowingId);

        // 2. ADIM: Kitap zaten iade edilmişse stoğu ikinci kez artırmamak için 409 dön
        if (borrowing.getStatus() == BorrowingStatus.RETURNED) {
            throw new BookAlreadyReturnedException(borrowingId);
        }

        // 3. ADIM: Book-Service'e git ve kitabın stoğunu 1 artır!
        bookClient.increaseStock(borrowing.getBookId());

        // 4. ADIM: Kendi veritabanımızda kaydı RETURNED olarak güncelle ve iade tarihini yaz
        borrowing.setStatus(BorrowingStatus.RETURNED);
        borrowing.setReturnDate(LocalDate.now());

        try {
            return borrowingRepository.save(borrowing);
        } catch (Exception ex) {
            // TELAFİ: Stok artırıldı ama iade kaydı güncellenemedi -> artırdığımız stoğu geri düşür
            log.error("İade kaydı güncellenemedi, stok tekrar düşürülüyor. borrowingId={}, bookId={}",
                    borrowingId, borrowing.getBookId(), ex);
            compensateDecreaseStock(borrowing.getBookId());
            throw new BorrowingFailedException("Kitap iade kaydı güncellenemedi, işlem geri alındı.");
        }
    }

    // Tüm ödünç kayıtlarını listele
    public List<Borrowing> getAllBorrowings() {
        return borrowingRepository.findAll();
    }

    // ID'ye göre ödünç kaydı getir (yoksa 404)
    public Borrowing getBorrowingById(Long borrowingId) {
        return borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new BorrowingNotFoundException(borrowingId));
    }

    // Düşürülen stoğu geri verir (ödünç alma başarısız olunca).
    private void compensateIncreaseStock(Long bookId) {
        try {
            bookClient.increaseStock(bookId);
        } catch (Exception compensationEx) {
            log.error("!!! TELAFİ BAŞARISIZ: bookId={} için stok geri verilemedi, ELLE DÜZELTİLMELİ!",
                    bookId, compensationEx);
        }
    }

    // Artırılan stoğu geri düşürür (iade kaydı yazılamazsa).
    private void compensateDecreaseStock(Long bookId) {
        try {
            bookClient.decreaseStock(bookId);
        } catch (Exception compensationEx) {
            log.error("!!! İADE TELAFİSİ BAŞARISIZ: bookId={} için artırılan stok geri alınamadı, ELLE DÜZELTİLMELİ!",
                    bookId, compensationEx);
        }
    }
}
