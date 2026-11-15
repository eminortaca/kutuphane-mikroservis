package com.kutuphane.bookservice.service;

import com.kutuphane.bookservice.dto.BookRequest;
import com.kutuphane.bookservice.entity.Book;
import com.kutuphane.bookservice.exception.BookNotFoundException;
import com.kutuphane.bookservice.exception.OutOfStockException;
import com.kutuphane.bookservice.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// Kitaplarla ilgili tüm iş mantığı burada toplanır; Controller sadece HTTP isteklerini karşılar.
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    // 1. Yeni kitap ekle
    public Book createBook(BookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setStock(request.getStock());
        return bookRepository.save(book);
    }

    // 2. Tüm kitapları listele
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // 3. ID'ye göre kitap getir (Kitap yoksa 404 döner -> GlobalExceptionHandler)
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    // 4. Kitap ödünç alındığında stoğu 1 düşür
    public Book decreaseStock(Long id) {
        Book book = getBookById(id); // Kitap hiç yoksa burada 404 fırlar

        // Kontrol + düşürme tek SQL'de (atomik). Aynı anda gelen isteklerde stok eksiye düşemez.
        int updatedRows = bookRepository.decreaseStockIfAvailable(id);
        if (updatedRows == 0) {
            throw new OutOfStockException(book.getTitle()); // 409 (Conflict) döner
        }
        return getBookById(id); // Güncel stokla birlikte geri dön
    }

    // 5. Stoğu 1 artır (İade veya telafi işlemi için)
    public Book increaseStock(Long id) {
        getBookById(id); // Kitap yoksa 404
        bookRepository.increaseStock(id);
        return getBookById(id);
    }
}
