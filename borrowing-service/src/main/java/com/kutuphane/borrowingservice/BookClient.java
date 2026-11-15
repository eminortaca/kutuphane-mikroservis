package com.kutuphane.borrowingservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

// Artık localhost veya 8081 portu YOK! Sadece servisin adı (book-service) var!
@FeignClient(name = "book-service", path = "/api/books")
public interface BookClient {

    @GetMapping("/{id}")
    BookDto getBookById(@PathVariable("id") Long id);

    @PutMapping("/{id}/decrease-stock")
    BookDto decreaseStock(@PathVariable("id") Long id);
}