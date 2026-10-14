package com.kutuphane.borrowingservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

// Şimdilik URL'i sabit (localhost:8081) veriyoruz.
// Bir sonraki fazda Eureka kurduğumuzda bu url="..." kısmını sileceğiz!
@FeignClient(name = "book-service", url = "http://localhost:8081/api/books")
public interface BookClient {

    @GetMapping("/{id}")
    BookDto getBookById(@PathVariable("id") Long id);

    @PutMapping("/{id}/decrease-stock")
    BookDto decreaseStock(@PathVariable("id") Long id);
}