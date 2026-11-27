package com.kutuphane.bookservice.config;

import com.kutuphane.bookservice.entity.Book;
import com.kutuphane.bookservice.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// Uygulama ilk kez boş veritabanıyla açıldığında örnek kitap kataloğunu otomatik yükler.
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final BookRepository bookRepository;

    @Override
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            return;
        }

        List<Book> sampleBooks = List.of(
                new Book(null, "Suç ve Ceza", "Fyodor Dostoyevski", "978-975-07-1938-7", "Roman", 1866, 4, 4),
                new Book(null, "1984", "George Orwell", "978-975-07-1853-3", "Distopya", 1949, 5, 5),
                new Book(null, "Kürk Mantolu Madonna", "Sabahattin Ali", "978-975-363-802-9", "Roman", 1943, 3, 3),
                new Book(null, "Simyacı", "Paulo Coelho", "978-975-07-2643-9", "Felsefe", 1988, 3, 3),
                new Book(null, "Clean Code", "Robert C. Martin", "978-0-13-235088-4", "Yazılım", 2008, 2, 2),
                new Book(null, "Sapiens", "Yuval Noah Harari", "978-605-09-2311-7", "Tarih", 2011, 4, 4)
        );

        bookRepository.saveAll(sampleBooks);
        log.info("Başlangıç verisi yüklendi: {} örnek kitap eklendi.", sampleBooks.size());
    }
}
