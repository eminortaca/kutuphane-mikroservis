package com.kutuphane.borrowingservice.controller;

import com.kutuphane.borrowingservice.dto.BorrowingResponse;
import com.kutuphane.borrowingservice.service.BorrowingService;
import jakarta.validation.constraints.NotBlank;
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
            @RequestParam @NotBlank(message = "Ödünç alan kişinin adı boş olamaz!") String borrowerName) {
        BorrowingResponse created = BorrowingResponse.from(borrowingService.borrowBook(bookId, borrowerName));
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

    // 3. Tüm ödünç kayıtlarını listele
    @GetMapping
    public List<BorrowingResponse> getAllBorrowings() {
        return borrowingService.getAllBorrowings().stream()
                .map(BorrowingResponse::from)
                .toList();
    }

    // 4. ID'ye göre ödünç kaydı getir
    @GetMapping("/{id}")
    public BorrowingResponse getBorrowingById(@PathVariable Long id) {
        return BorrowingResponse.from(borrowingService.getBorrowingById(id));
    }
}
