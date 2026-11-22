package com.kutuphane.borrowingservice.service;

import com.kutuphane.borrowingservice.client.BookClient;
import com.kutuphane.borrowingservice.client.MemberClient;
import com.kutuphane.borrowingservice.dto.BookDto;
import com.kutuphane.borrowingservice.dto.MemberDto;
import com.kutuphane.borrowingservice.entity.Borrowing;
import com.kutuphane.borrowingservice.entity.BorrowingStatus;
import com.kutuphane.borrowingservice.exception.BookAlreadyReturnedException;
import com.kutuphane.borrowingservice.exception.BorrowingAccessDeniedException;
import com.kutuphane.borrowingservice.exception.BorrowingFailedException;
import com.kutuphane.borrowingservice.exception.BorrowingNotFoundException;
import com.kutuphane.borrowingservice.exception.BorrowingRuleViolationException;
import com.kutuphane.borrowingservice.repository.BorrowingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// Ödünç alma, süre uzatma, gecikme takibi ve iade iş mantığı burada toplanır.
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowingService {

    private static final List<BorrowingStatus> ACTIVE_STATUSES =
            List.of(BorrowingStatus.BORROWED, BorrowingStatus.OVERDUE);

    private final BorrowingRepository borrowingRepository;
    private final BookClient bookClient;
    private final MemberClient memberClient;

    @Value("${borrowing.default-duration-days:14}")
    private int defaultDurationDays;

    @Value("${borrowing.renew-duration-days:7}")
    private int renewDurationDays;

    @Value("${borrowing.max-active-per-member:3}")
    private int maxActivePerMember;

    @Value("${borrowing.daily-late-fee:5.0}")
    private double dailyLateFee;

    // Kitap ödünç alma işlemi
    public Borrowing borrowBook(Long bookId, Long memberId, String borrowerName, Integer durationDays) {
        if (memberId == null && (borrowerName == null || borrowerName.isBlank())) {
            throw new IllegalArgumentException("Üye ID (memberId) veya ödünç alan adı (borrowerName) belirtilmelidir!");
        }

        // Gecikmiş kayıtların güncel olduğundan emin ol
        markOverdueBorrowings();

        // 1. ADIM: Üye ve ödünç alma kurallarını doğrula
        String resolvedBorrowerName = validateBorrowerAndResolveName(bookId, memberId, borrowerName);

        // 2. ADIM: Book-Service'e git ve kitabın stoğunu atomik olarak 1 düşür!
        // Kitap yoksa 404, stok yoksa 409 döner.
        BookDto book = bookClient.decreaseStock(bookId);

        // 3. ADIM: Kendi veritabanımıza (borrowingdb) ödünç alma kaydını yaz!
        int days = (durationDays != null) ? durationDays : defaultDurationDays;
        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plusDays(days);
        boolean isAlreadyOverdue = borrowDate.isAfter(dueDate);

        Borrowing borrowing = new Borrowing();
        borrowing.setBookId(book.getId());
        borrowing.setBookTitle(book.getTitle());
        borrowing.setMemberId(memberId);
        borrowing.setBorrowerName(resolvedBorrowerName);
        borrowing.setBorrowDate(borrowDate);
        borrowing.setDueDate(dueDate);
        borrowing.setReturnDate(null);
        borrowing.setRenewCount(0);
        borrowing.setLateFee(isAlreadyOverdue ? calculateLateFee(dueDate, borrowDate) : 0.0);
        borrowing.setStatus(isAlreadyOverdue ? BorrowingStatus.OVERDUE : BorrowingStatus.BORROWED);

        try {
            return borrowingRepository.save(borrowing);
        } catch (Exception ex) {
            // 4. ADIM (TELAFİ): Stok düştü ama kayıt yazılamadı!
            log.error("Ödünç kaydı yazılamadı, stok geri veriliyor. bookId={}", bookId, ex);
            compensateIncreaseStock(bookId);
            throw new BorrowingFailedException("Ödünç alma kaydı oluşturulamadı, işlem geri alındı.");
        }
    }

    // Ödünç süresini uzatma (max 1 kez)
    public Borrowing renewBorrowing(Long borrowingId) {
        return renewBorrowing(borrowingId, null, null);
    }

    public Borrowing renewBorrowing(Long borrowingId, String authRole, Long authMemberId) {
        markOverdueBorrowings();

        Borrowing borrowing = findBorrowingOrThrow(borrowingId);
        validateOwnership(borrowing, authRole, authMemberId);

        if (borrowing.getStatus() == BorrowingStatus.RETURNED) {
            throw new BookAlreadyReturnedException(borrowingId);
        }
        if (borrowing.getStatus() == BorrowingStatus.OVERDUE
                || (borrowing.getDueDate() != null && LocalDate.now().isAfter(borrowing.getDueDate()))) {
            throw new BorrowingRuleViolationException("Teslim süresi geçmiş (gecikmiş) kitapların süresi uzatılamaz!");
        }

        int currentRenewCount = borrowing.getRenewCount() != null ? borrowing.getRenewCount() : 0;
        if (currentRenewCount >= 1) {
            throw new BorrowingRuleViolationException("Bir kitabın ödünç süresi en fazla 1 kez uzatılabilir!");
        }

        LocalDate baseDueDate = borrowing.getDueDate() != null ? borrowing.getDueDate() : LocalDate.now();
        borrowing.setDueDate(baseDueDate.plusDays(renewDurationDays));
        borrowing.setRenewCount(currentRenewCount + 1);
        return borrowingRepository.save(borrowing);
    }

    // Kitap iade etme işlemi
    public Borrowing returnBook(Long borrowingId) {
        return returnBook(borrowingId, null, null);
    }

    public Borrowing returnBook(Long borrowingId, String authRole, Long authMemberId) {
        Borrowing borrowing = findBorrowingOrThrow(borrowingId);
        validateOwnership(borrowing, authRole, authMemberId);

        if (borrowing.getStatus() == BorrowingStatus.RETURNED) {
            throw new BookAlreadyReturnedException(borrowingId);
        }

        // Book-Service'e git ve kitabın stoğunu 1 artır!
        bookClient.increaseStock(borrowing.getBookId());

        LocalDate returnDate = LocalDate.now();
        borrowing.setStatus(BorrowingStatus.RETURNED);
        borrowing.setReturnDate(returnDate);
        borrowing.setLateFee(calculateLateFee(borrowing.getDueDate(), returnDate));

        try {
            return borrowingRepository.save(borrowing);
        } catch (ObjectOptimisticLockingFailureException ex) {
            // TELAFİ: Stok artırıldı ama eşzamanlı iade çakıştı -> artırılan stoğu geri düşür ve 409 fırlat
            log.error("Eşzamanlı iade çakışması, stok tekrar düşürülüyor. borrowingId={}, bookId={}",
                    borrowingId, borrowing.getBookId(), ex);
            compensateDecreaseStock(borrowing.getBookId());
            throw ex;
        } catch (Exception ex) {
            // TELAFİ: Stok artırıldı ama iade kaydı güncellenemedi -> artırılan stoğu geri düşür
            log.error("İade kaydı güncellenemedi, stok tekrar düşürülüyor. borrowingId={}, bookId={}",
                    borrowingId, borrowing.getBookId(), ex);
            compensateDecreaseStock(borrowing.getBookId());
            throw new BorrowingFailedException("Kitap iade kaydı güncellenemedi, işlem geri alındı.");
        }
    }

    // Ödünç kayıtlarını opsiyonel status ve memberId filtreleriyle listele
    public List<Borrowing> getAllBorrowings(BorrowingStatus status, Long memberId) {
        markOverdueBorrowings();
        return borrowingRepository.findWithFilters(status, memberId);
    }

    // ID'ye göre ödünç kaydı getir (yoksa 404)
    public Borrowing getBorrowingById(Long borrowingId) {
        return getBorrowingById(borrowingId, null, null);
    }

    public Borrowing getBorrowingById(Long borrowingId, String authRole, Long authMemberId) {
        markOverdueBorrowings();
        Borrowing borrowing = findBorrowingOrThrow(borrowingId);
        validateOwnership(borrowing, authRole, authMemberId);
        return borrowing;
    }

    private void validateOwnership(Borrowing borrowing, String authRole, Long authMemberId) {
        if ("MEMBER".equalsIgnoreCase(authRole)
                && (authMemberId == null || !authMemberId.equals(borrowing.getMemberId()))) {
            throw new BorrowingAccessDeniedException("Bu ödünç kaydına erişim yetkiniz yok!");
        }
    }

    // Periyodik olarak (dakikada bir) teslim tarihi geçmiş kayıtları OVERDUE olarak işaretler
    @Scheduled(fixedRate = 60000)
    public void markOverdueBorrowings() {
        LocalDate today = LocalDate.now();
        List<Borrowing> activeBorrowings = borrowingRepository.findByStatusIn(ACTIVE_STATUSES);
        List<Borrowing> updated = new ArrayList<>();

        for (Borrowing borrowing : activeBorrowings) {
            if (borrowing.getDueDate() != null && today.isAfter(borrowing.getDueDate())) {
                double fee = calculateLateFee(borrowing.getDueDate(), today);
                if (borrowing.getStatus() != BorrowingStatus.OVERDUE
                        || borrowing.getLateFee() == null
                        || Double.compare(borrowing.getLateFee(), fee) != 0) {
                    borrowing.setStatus(BorrowingStatus.OVERDUE);
                    borrowing.setLateFee(fee);
                    updated.add(borrowing);
                }
            }
        }

        if (!updated.isEmpty()) {
            borrowingRepository.saveAll(updated);
            log.info("{} adet gecikmiş ödünç kaydı OVERDUE olarak güncellendi.", updated.size());
        }
    }

    private Borrowing findBorrowingOrThrow(Long borrowingId) {
        return borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new BorrowingNotFoundException(borrowingId));
    }

    private String validateBorrowerAndResolveName(Long bookId, Long memberId, String borrowerName) {
        if (memberId != null) {
            MemberDto member = memberClient.getMemberById(memberId);
            if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
                throw new BorrowingRuleViolationException(
                        "Üyeliğiniz aktif olmadığı için kitap ödünç alamazsınız! Durum: " + member.getStatus());
            }

            boolean hasOverdue = borrowingRepository.existsByMemberIdAndStatus(memberId, BorrowingStatus.OVERDUE);
            boolean hasSameBook = borrowingRepository.existsByMemberIdAndBookIdAndStatusIn(memberId, bookId, ACTIVE_STATUSES);
            long activeCount = borrowingRepository.countByMemberIdAndStatusIn(memberId, ACTIVE_STATUSES);
            assertBorrowerRules(hasOverdue, hasSameBook, activeCount, bookId);
            return member.getName();
        }

        String normalizedName = borrowerName.trim();
        boolean hasOverdue = borrowingRepository.existsByBorrowerNameIgnoreCaseAndStatus(normalizedName, BorrowingStatus.OVERDUE);
        boolean hasSameBook = borrowingRepository.existsByBorrowerNameIgnoreCaseAndBookIdAndStatusIn(
                normalizedName, bookId, ACTIVE_STATUSES);
        long activeCount = borrowingRepository.countByBorrowerNameIgnoreCaseAndStatusIn(normalizedName, ACTIVE_STATUSES);
        assertBorrowerRules(hasOverdue, hasSameBook, activeCount, bookId);
        return normalizedName;
    }

    private void assertBorrowerRules(boolean hasOverdue, boolean hasSameBook, long activeCount, Long bookId) {
        if (hasOverdue) {
            throw new BorrowingRuleViolationException(
                    "Gecikmiş (iade süresi geçmiş) kitabınız bulunduğu için yeni kitap ödünç alamazsınız!");
        }
        if (hasSameBook) {
            throw new BorrowingRuleViolationException(
                    "Bu kitabı zaten ödünç aldınız ve henüz iade etmediniz! Kitap ID: " + bookId);
        }
        if (activeCount >= maxActivePerMember) {
            throw new BorrowingRuleViolationException(
                    "Bir üye aynı anda en fazla " + maxActivePerMember + " kitap ödünç alabilir!");
        }
    }

    private double calculateLateFee(LocalDate dueDate, LocalDate referenceDate) {
        if (dueDate == null || !referenceDate.isAfter(dueDate)) {
            return 0.0;
        }
        long overdueDays = ChronoUnit.DAYS.between(dueDate, referenceDate);
        return overdueDays * dailyLateFee;
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
