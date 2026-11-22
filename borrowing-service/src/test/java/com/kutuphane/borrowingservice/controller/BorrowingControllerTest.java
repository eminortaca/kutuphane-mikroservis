package com.kutuphane.borrowingservice.controller;

import com.kutuphane.borrowingservice.entity.Borrowing;
import com.kutuphane.borrowingservice.entity.BorrowingStatus;
import com.kutuphane.borrowingservice.exception.BookAlreadyReturnedException;
import com.kutuphane.borrowingservice.exception.BookServiceUnavailableException;
import com.kutuphane.borrowingservice.exception.BorrowingAccessDeniedException;
import com.kutuphane.borrowingservice.exception.BorrowingRuleViolationException;
import com.kutuphane.borrowingservice.service.BorrowingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BorrowingController.class)
class BorrowingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorrowingService borrowingService;

    @Test
    void borrowBook_whenValid_shouldReturn201() throws Exception {
        Borrowing saved = new Borrowing(1L, 10L, "Suç ve Ceza", 1L, "Ahmet Yılmaz",
                LocalDate.now(), LocalDate.now().plusDays(14), null, 0, 0.0, BorrowingStatus.BORROWED);
        when(borrowingService.borrowBook(10L, 1L, null, null)).thenReturn(saved);

        mockMvc.perform(post("/api/borrowings")
                        .param("bookId", "10")
                        .param("memberId", "1"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/borrowings/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BORROWED"));
    }

    @Test
    void borrowBook_whenRuleViolated_shouldReturn409() throws Exception {
        when(borrowingService.borrowBook(10L, 1L, null, null))
                .thenThrow(new BorrowingRuleViolationException("Bir üye aynı anda en fazla 3 kitap ödünç alabilir!"));

        mockMvc.perform(post("/api/borrowings")
                        .param("bookId", "10")
                        .param("memberId", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void borrowBook_whenBookServiceDown_shouldReturn503() throws Exception {
        when(borrowingService.borrowBook(10L, 1L, null, null))
                .thenThrow(new BookServiceUnavailableException("Kitap servisine şu an ulaşılamıyor"));

        mockMvc.perform(post("/api/borrowings")
                        .param("bookId", "10")
                        .param("memberId", "1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    @Test
    void returnBook_whenAlreadyReturned_shouldReturn409() throws Exception {
        when(borrowingService.returnBook(1L, null, null)).thenThrow(new BookAlreadyReturnedException(1L));

        mockMvc.perform(put("/api/borrowings/1/return"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Bu kitap zaten iade edilmiş! Ödünç ID: 1"));
    }

    @Test
    void returnBook_whenMemberTriesAnotherMembersBorrowing_shouldReturn403() throws Exception {
        when(borrowingService.returnBook(1L, "MEMBER", 2L))
                .thenThrow(new BorrowingAccessDeniedException("Bu ödünç kaydına erişim yetkiniz yok!"));

        mockMvc.perform(put("/api/borrowings/1/return")
                        .header("X-Auth-Role", "MEMBER")
                        .header("X-Auth-Member-Id", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Bu ödünç kaydına erişim yetkiniz yok!"));
    }

    @Test
    void renewBorrowing_whenOptimisticLockConflict_shouldReturn409() throws Exception {
        when(borrowingService.renewBorrowing(1L, null, null))
                .thenThrow(new ObjectOptimisticLockingFailureException(Borrowing.class, 1L));

        mockMvc.perform(put("/api/borrowings/1/renew"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}
