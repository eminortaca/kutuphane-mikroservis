package com.kutuphane.borrowingservice.controller;

import com.kutuphane.borrowingservice.entity.Borrowing;
import com.kutuphane.borrowingservice.service.BorrowingService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/borrowings")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingService borrowingService;

    // 1. Kitap ödünç al (Parametreler @Validated ile doğrulanır)
    @PostMapping
    public Borrowing borrowBook(
            @RequestParam @NotNull(message = "Kitap ID belirtilmelidir!") @Positive(message = "Kitap ID pozitif bir sayı olmalıdır!") Long bookId,
            @RequestParam @NotBlank(message = "Ödünç alan kişinin adı boş olamaz!") String borrowerName) {
        return borrowingService.borrowBook(bookId, borrowerName);
    }

    // 2. Kitap iade et
    @PutMapping("/{id}/return")
    public Borrowing returnBook(@PathVariable Long id) {
        return borrowingService.returnBook(id);
    }

    // 3. Tüm ödünç kayıtlarını listele
    @GetMapping
    public List<Borrowing> getAllBorrowings() {
        return borrowingService.getAllBorrowings();
    }
}
