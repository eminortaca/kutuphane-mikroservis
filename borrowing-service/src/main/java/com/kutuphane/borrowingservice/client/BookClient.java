package com.kutuphane.borrowingservice.client;

import com.kutuphane.borrowingservice.dto.BookDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

// Artık localhost veya 8081 portu YOK! Sadece servisin adı (book-service) var!
// path = "/internal/books": Stok uç noktaları Book-Service'te servisler arası (internal) yolda durur.
// Bu yol API Gateway'de yönlendirilmediği için dış dünya stoğu değiştiremez.
// fallbackFactory: Book-Service çökerse / yavaşlarsa / devre kesici açılırsa ne yapılacağını söyler.
@FeignClient(name = "book-service", path = "/internal/books", fallbackFactory = BookClientFallbackFactory.class)
public interface BookClient {

    // Stoğu 1 düşürür ve güncel kitap bilgisini döner.
    // Kitap yoksa 404, stok yoksa 409 döner -> ayrı bir "kitabı getir" çağrısına gerek yok.
    @PutMapping("/{id}/decrease-stock")
    BookDto decreaseStock(@PathVariable("id") Long id);

    // İADE veya TELAFİ: Stoğu 1 artırır
    @PutMapping("/{id}/increase-stock")
    BookDto increaseStock(@PathVariable("id") Long id);
}
