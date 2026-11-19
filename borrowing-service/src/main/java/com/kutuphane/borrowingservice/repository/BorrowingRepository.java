package com.kutuphane.borrowingservice.repository;

import com.kutuphane.borrowingservice.entity.Borrowing;
import com.kutuphane.borrowingservice.entity.BorrowingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {

    List<Borrowing> findByStatusIn(Collection<BorrowingStatus> statuses);

    // Üye ID bazlı kurallar
    long countByMemberIdAndStatusIn(Long memberId, Collection<BorrowingStatus> statuses);

    boolean existsByMemberIdAndBookIdAndStatusIn(Long memberId, Long bookId, Collection<BorrowingStatus> statuses);

    boolean existsByMemberIdAndStatus(Long memberId, BorrowingStatus status);

    // Geriye dönük uyumluluk: Yalnızca borrowerName gönderilen istekler için
    long countByBorrowerNameIgnoreCaseAndStatusIn(String borrowerName, Collection<BorrowingStatus> statuses);

    boolean existsByBorrowerNameIgnoreCaseAndBookIdAndStatusIn(String borrowerName, Long bookId, Collection<BorrowingStatus> statuses);

    boolean existsByBorrowerNameIgnoreCaseAndStatus(String borrowerName, BorrowingStatus status);

    // Durum ve/veya üye ID'ye göre filtreleme
    @Query("""
            SELECT b FROM Borrowing b
            WHERE (:status IS NULL OR b.status = :status)
              AND (:memberId IS NULL OR b.memberId = :memberId)
            """)
    List<Borrowing> findWithFilters(
            @Param("status") BorrowingStatus status,
            @Param("memberId") Long memberId);
}
