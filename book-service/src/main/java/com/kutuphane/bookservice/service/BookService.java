package com.kutuphane.bookservice.service;

import com.kutuphane.bookservice.dto.BookRequest;
import com.kutuphane.bookservice.entity.Book;
import com.kutuphane.bookservice.exception.BookInUseException;
import com.kutuphane.bookservice.exception.BookNotFoundException;
import com.kutuphane.bookservice.exception.DuplicateIsbnException;
import com.kutuphane.bookservice.exception.OutOfStockException;
import com.kutuphane.bookservice.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

// Kitaplarla ilgili tüm iş mantığı burada toplanır; Controller sadece HTTP isteklerini karşılar.
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    // 1. Yeni kitap ekle
    public Book createBook(BookRequest request) {
        String isbn = normalizeOptional(request.getIsbn());
        if (isbn != null && bookRepository.existsByIsbnIgnoreCase(isbn)) {
            throw new DuplicateIsbnException(isbn);
        }

        int stock = request.getStock();
        int totalCopies = (request.getTotalCopies() != null && request.getTotalCopies() >= stock)
                ? request.getTotalCopies()
                : stock;

        Book book = new Book();
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setIsbn(isbn);
        book.setCategory(normalizeOptional(request.getCategory()));
        book.setPublishYear(request.getPublishYear());
        book.setStock(stock);
        book.setTotalCopies(totalCopies);
        return bookRepository.save(book);
    }

    // 2. Kitapları filtrele ve sayfalı getir
    public Page<Book> searchBooks(String title, String author, String category, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        return bookRepository.searchBooks(
                normalizeOptional(title),
                normalizeOptional(author),
                normalizeOptional(category),
                PageRequest.of(safePage, safeSize));
    }

    // 4. ID'ye göre kitap getir (Kitap yoksa 404 döner -> GlobalExceptionHandler)
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    // 5. Kitap güncelle
    public Book updateBook(Long id, BookRequest request) {
        Book book = getBookById(id);

        String isbn = normalizeOptional(request.getIsbn());
        if (isbn != null && bookRepository.existsByIsbnIgnoreCaseAndIdNot(isbn, id)) {
            throw new DuplicateIsbnException(isbn);
        }

        int borrowedCopies = getBorrowedCopies(book);
        if (request.getTotalCopies() != null) {
            if (request.getTotalCopies() < borrowedCopies) {
                throw new BookInUseException(
                        "Bu kitabın şu anda ödünçte olan " + borrowedCopies
                                + " kopyası var. Toplam kopya sayısı bunun altına düşürülemez!");
            }
            book.setTotalCopies(request.getTotalCopies());
            book.setStock(request.getTotalCopies() - borrowedCopies);
        } else {
            book.setStock(request.getStock());
            book.setTotalCopies(request.getStock() + borrowedCopies);
        }

        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setIsbn(isbn);
        book.setCategory(normalizeOptional(request.getCategory()));
        book.setPublishYear(request.getPublishYear());
        return bookRepository.save(book);
    }

    // 6. Kitap sil (aktif ödünçte kopya varsa engelle)
    public void deleteBook(Long id) {
        Book book = getBookById(id);
        int borrowedCopies = getBorrowedCopies(book);
        if (borrowedCopies > 0) {
            throw new BookInUseException(
                    "Bu kitabın şu anda ödünçte olan " + borrowedCopies + " kopyası bulunduğu için silinemez!");
        }
        bookRepository.delete(book);
    }

    // 7. Kitap ödünç alındığında stoğu 1 düşür
    public Book decreaseStock(Long id) {
        Book book = getBookById(id); // Kitap hiç yoksa burada 404 fırlar

        // Kontrol + düşürme tek SQL'de (atomik). Aynı anda gelen isteklerde stok eksiye düşemez.
        int updatedRows = bookRepository.decreaseStockIfAvailable(id);
        if (updatedRows == 0) {
            throw new OutOfStockException(book.getTitle()); // 409 (Conflict) döner
        }
        return getBookById(id); // Güncel stokla birlikte geri dön
    }

    // 8. Stoğu 1 artır (İade veya telafi işlemi için)
    public Book increaseStock(Long id) {
        int updatedRows = bookRepository.increaseStock(id);
        if (updatedRows == 0) {
            throw new BookNotFoundException(id);
        }
        return getBookById(id);
    }

    private int getBorrowedCopies(Book book) {
        int currentTotal = book.getTotalCopies() != null ? book.getTotalCopies() : book.getStock();
        return Math.max(0, currentTotal - book.getStock());
    }

    private String normalizeOptional(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
