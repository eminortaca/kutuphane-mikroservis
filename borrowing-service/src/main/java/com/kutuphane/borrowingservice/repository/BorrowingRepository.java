package com.kutuphane.borrowingservice.repository;

import com.kutuphane.borrowingservice.entity.Borrowing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
}