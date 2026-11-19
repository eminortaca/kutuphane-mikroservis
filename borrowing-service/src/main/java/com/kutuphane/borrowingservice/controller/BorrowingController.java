package com.kutuphane.borrowingservice.controller;

import com.kutuphane.borrowingservice.dto.BorrowingResponse;
import com.kutuphane.borrowingservice.entity.BorrowingStatus;
import com.kutuphane.borrowingservice.service.BorrowingService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/borrowings")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingService borrowingService;

    // 1. Kitap ödünç al -> 201 Created + Location: /api/borrowings/{id}
    @PostMapping
    public ResponseEntity<BorrowingResponse> borrowBook(
            @RequestParam @NotNull(message = "Kitap ID belirtilmelidir!") @Positive(message = "Kitap ID pozitif bir sayı olmalıdır!") Long bookId,
            @RequestParam(required = false) @Positive(message = "Üye ID pozitif bir sayı olmalıdır!") Long memberId,
            @RequestParam(required = false) String borrowerName,
            @RequestParam(required = false) Integer durationDays) {
        BorrowingResponse created = BorrowingResponse.from(
                borrowingService.borrowBook(bookId, memberId, borrowerName, durationDays));
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // 2. Kitap iade et
    @PutMapping("/{id}/return")
    public BorrowingResponse returnBook(@PathVariable Long id) {
        return BorrowingResponse.from(borrowingService.returnBook(id));
    }

    // 3. Ödünç süresini uzat (max 1 kez)
    @PutMapping("/{id}/renew")
    public BorrowingResponse renewBorrowing(@PathVariable Long id) {
        return BorrowingResponse.from(borrowingService.renewBorrowing(id));
    }

    // 4. Ödünç kayıtlarını listele / filtrele (?status=&memberId=)
    @GetMapping
    public List<BorrowingResponse> getAllBorrowings(
            @RequestParam(required = false) BorrowingStatus status,
            @RequestParam(required = false) Long memberId) {
        return borrowingService.getAllBorrowings(status, memberId).stream()
                .map(BorrowingResponse::from)
                .toList();
    }

    // 5. ID'ye göre ödünç kaydı getir
    @GetMapping("/{id}")
    public BorrowingResponse getBorrowingById(@PathVariable Long id) {
        return BorrowingResponse.from(borrowingService.getBorrowingById(id));
    }
}
