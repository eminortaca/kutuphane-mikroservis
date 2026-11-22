package com.kutuphane.borrowingservice.service;

import com.kutuphane.borrowingservice.client.BookClient;
import com.kutuphane.borrowingservice.client.MemberClient;
import com.kutuphane.borrowingservice.dto.BookDto;
import com.kutuphane.borrowingservice.dto.MemberDto;
import com.kutuphane.borrowingservice.entity.Borrowing;
import com.kutuphane.borrowingservice.entity.BorrowingStatus;
import com.kutuphane.borrowingservice.exception.BorrowingAccessDeniedException;
import com.kutuphane.borrowingservice.exception.BorrowingFailedException;
import com.kutuphane.borrowingservice.exception.BorrowingRuleViolationException;
import com.kutuphane.borrowingservice.repository.BorrowingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowingServiceTest {

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private BookClient bookClient;

    @Mock
    private MemberClient memberClient;

    @InjectMocks
    private BorrowingService borrowingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(borrowingService, "defaultDurationDays", 14);
        ReflectionTestUtils.setField(borrowingService, "renewDurationDays", 7);
        ReflectionTestUtils.setField(borrowingService, "maxActivePerMember", 3);
        ReflectionTestUtils.setField(borrowingService, "dailyLateFee", 5.0);
        lenient().when(borrowingRepository.findByStatusIn(anyCollection())).thenReturn(List.of());
    }

    @Test
    void borrowBook_whenValidMemberAndBook_shouldDecreaseStockAndSaveBorrowing() {
        when(memberClient.getMemberById(1L))
                .thenReturn(new MemberDto(1L, "Ahmet Yılmaz", "ahmet@example.com", "0555", "ACTIVE"));
        when(bookClient.decreaseStock(10L))
                .thenReturn(new BookDto(10L, "Suç ve Ceza", "Dostoyevski", 2));
        when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(inv -> {
            Borrowing b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        Borrowing result = borrowingService.borrowBook(10L, 1L, null, null);

        assertEquals(1L, result.getId());
        assertEquals(BorrowingStatus.BORROWED, result.getStatus());
        assertEquals(LocalDate.now().plusDays(14), result.getDueDate());
        assertEquals("Ahmet Yılmaz", result.getBorrowerName());
        verify(bookClient).decreaseStock(10L);
    }

    @Test
    void borrowBook_whenDatabaseSaveFails_shouldCompensateByIncreasingStock() {
        when(memberClient.getMemberById(1L))
                .thenReturn(new MemberDto(1L, "Ahmet Yılmaz", "ahmet@example.com", "0555", "ACTIVE"));
        when(bookClient.decreaseStock(10L))
                .thenReturn(new BookDto(10L, "Suç ve Ceza", "Dostoyevski", 2));
        when(borrowingRepository.save(any(Borrowing.class)))
                .thenThrow(new RuntimeException("DB bağlantı hatası"));

        assertThrows(BorrowingFailedException.class,
                () -> borrowingService.borrowBook(10L, 1L, null, null));

        // SAGA TELAFİSİ: Kayıt yazılamadığı için düşürülen stok geri artırılmalı
        verify(bookClient).increaseStock(10L);
    }

    @Test
    void returnBook_whenDatabaseSaveFails_shouldCompensateByDecreasingStock() {
        Borrowing existing = new Borrowing(1L, 10L, "Suç ve Ceza", 1L, "Ahmet",
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(12), null, 0, 0.0, BorrowingStatus.BORROWED);
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(borrowingRepository.save(any(Borrowing.class)))
                .thenThrow(new RuntimeException("DB güncelleme hatası"));

        assertThrows(BorrowingFailedException.class, () -> borrowingService.returnBook(1L));

        // SAGA TELAFİSİ: İade kaydı yazılamadığı için artırılan stok geri düşürülmeli
        verify(bookClient).increaseStock(10L);
        verify(bookClient).decreaseStock(10L);
    }

    @Test
    void borrowBook_whenMemberSuspended_shouldThrowBorrowingRuleViolationException() {
        when(memberClient.getMemberById(1L))
                .thenReturn(new MemberDto(1L, "Ahmet Yılmaz", "ahmet@example.com", "0555", "SUSPENDED"));

        assertThrows(BorrowingRuleViolationException.class,
                () -> borrowingService.borrowBook(10L, 1L, null, null));

        verify(bookClient, never()).decreaseStock(anyLong());
    }

    @Test
    void borrowBook_whenMemberHasOverdueBook_shouldThrowBorrowingRuleViolationException() {
        when(memberClient.getMemberById(1L))
                .thenReturn(new MemberDto(1L, "Ahmet Yılmaz", "ahmet@example.com", "0555", "ACTIVE"));
        when(borrowingRepository.existsByMemberIdAndStatus(1L, BorrowingStatus.OVERDUE)).thenReturn(true);

        assertThrows(BorrowingRuleViolationException.class,
                () -> borrowingService.borrowBook(10L, 1L, null, null));

        verify(bookClient, never()).decreaseStock(anyLong());
    }

    @Test
    void borrowBook_whenMemberAlreadyBorrowedSameBook_shouldThrowBorrowingRuleViolationException() {
        when(memberClient.getMemberById(1L))
                .thenReturn(new MemberDto(1L, "Ahmet Yılmaz", "ahmet@example.com", "0555", "ACTIVE"));
        when(borrowingRepository.existsByMemberIdAndBookIdAndStatusIn(eq(1L), eq(10L), anyCollection()))
                .thenReturn(true);

        assertThrows(BorrowingRuleViolationException.class,
                () -> borrowingService.borrowBook(10L, 1L, null, null));

        verify(bookClient, never()).decreaseStock(anyLong());
    }

    @Test
    void borrowBook_whenMemberReachedMaxActiveLimit_shouldThrowBorrowingRuleViolationException() {
        when(memberClient.getMemberById(1L))
                .thenReturn(new MemberDto(1L, "Ahmet Yılmaz", "ahmet@example.com", "0555", "ACTIVE"));
        when(borrowingRepository.countByMemberIdAndStatusIn(eq(1L), anyCollection())).thenReturn(3L);

        assertThrows(BorrowingRuleViolationException.class,
                () -> borrowingService.borrowBook(10L, 1L, null, null));

        verify(bookClient, never()).decreaseStock(anyLong());
    }

    @Test
    void renewBorrowing_firstTime_shouldExtendDueDateAndIncrementRenewCount() {
        LocalDate initialDue = LocalDate.now().plusDays(5);
        Borrowing existing = new Borrowing(1L, 10L, "Suç ve Ceza", 1L, "Ahmet",
                LocalDate.now().minusDays(9), initialDue, null, 0, 0.0, BorrowingStatus.BORROWED);
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrowing renewed = borrowingService.renewBorrowing(1L);

        assertEquals(initialDue.plusDays(7), renewed.getDueDate());
        assertEquals(1, renewed.getRenewCount());
    }

    @Test
    void renewBorrowing_secondTime_shouldThrowBorrowingRuleViolationException() {
        Borrowing existing = new Borrowing(1L, 10L, "Suç ve Ceza", 1L, "Ahmet",
                LocalDate.now().minusDays(9), LocalDate.now().plusDays(12), null, 1, 0.0, BorrowingStatus.BORROWED);
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(BorrowingRuleViolationException.class, () -> borrowingService.renewBorrowing(1L));
        verify(borrowingRepository, never()).save(any());
    }

    @Test
    void returnBook_whenOverdue_shouldCalculateLateFee() {
        Borrowing existing = new Borrowing(1L, 10L, "Suç ve Ceza", 1L, "Ahmet",
                LocalDate.now().minusDays(17), LocalDate.now().minusDays(3), null, 0, 15.0, BorrowingStatus.OVERDUE);
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrowing returned = borrowingService.returnBook(1L);

        assertEquals(BorrowingStatus.RETURNED, returned.getStatus());
        assertEquals(LocalDate.now(), returned.getReturnDate());
        assertEquals(15.0, returned.getLateFee());
        verify(bookClient).increaseStock(10L);
    }

    @Test
    void returnBook_whenMemberDoesNotOwnBorrowing_shouldThrowAccessDeniedAndNotTouchStock() {
        Borrowing existing = new Borrowing(1L, 10L, "Suç ve Ceza", 1L, "Ahmet",
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(12), null, 0, 0.0, BorrowingStatus.BORROWED);
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(BorrowingAccessDeniedException.class,
                () -> borrowingService.returnBook(1L, "MEMBER", 2L));

        verify(bookClient, never()).increaseStock(anyLong());
        verify(borrowingRepository, never()).save(any());
    }

    @Test
    void getBorrowingById_whenMemberDoesNotOwnBorrowing_shouldThrowAccessDenied() {
        Borrowing existing = new Borrowing(1L, 10L, "Suç ve Ceza", 1L, "Ahmet",
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(12), null, 0, 0.0, BorrowingStatus.BORROWED);
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(BorrowingAccessDeniedException.class,
                () -> borrowingService.getBorrowingById(1L, "MEMBER", 2L));
    }
}
