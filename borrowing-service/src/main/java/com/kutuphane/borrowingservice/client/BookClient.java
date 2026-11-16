package com.kutuphane.borrowingservice.client;

import com.kutuphane.borrowingservice.dto.BookDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

// Artık localhost veya 8081 portu YOK! Sadece servisin adı (book-service) var!
// fallbackFactory: Book-Service çökerse / yavaşlarsa / devre kesici açılırsa ne yapılacağını söyler.
@FeignClient(name = "book-service", path = "/api/books", fallbackFactory = BookClientFallbackFactory.class)
public interface BookClient {

    @GetMapping("/{id}")
    BookDto getBookById(@PathVariable("id") Long id);

    @PutMapping("/{id}/decrease-stock")
    BookDto decreaseStock(@PathVariable("id") Long id);

    // TELAFİ: Ödünç kaydı yazılamazsa düşürülen stoğu geri vermek için
    @PutMapping("/{id}/increase-stock")
    BookDto increaseStock(@PathVariable("id") Long id);
}
