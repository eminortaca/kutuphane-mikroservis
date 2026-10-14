package com.kutuphane.borrowingservice;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/borrowings")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingRepository borrowingRepository;
    private final BookClient bookClient; // Feign Client'ı enjekte ettik!

    @PostMapping
    public Borrowing borrowBook(@RequestParam Long bookId, @RequestParam String borrowerName) {

        // 1. ADIM: Book-Service'e (8081) git ve kitabı kontrol et!
        BookDto book = bookClient.getBookById(bookId);

        if (book.getStock() <= 0) {
            throw new RuntimeException("Üzgünüz, '" + book.getTitle() + "' kitabının stoğu kalmamış!");
        }

        // 2. ADIM: Book-Service'e (8081) git ve kitabın stoğunu 1 düşür!
        bookClient.decreaseStock(bookId);

        // 3. ADIM: Kendi veritabanımıza (8082 - borrowingdb) ödünç alma kaydını yaz!
        Borrowing borrowing = new Borrowing();
        borrowing.setBookId(book.getId());
        borrowing.setBookTitle(book.getTitle());
        borrowing.setBorrowerName(borrowerName);
        borrowing.setBorrowDate(LocalDate.now());

        return borrowingRepository.save(borrowing);
    }

    @GetMapping
    public List<Borrowing> getAllBorrowings() {
        return borrowingRepository.findAll();
    }
}