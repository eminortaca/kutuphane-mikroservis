package com.kutuphane.borrowingservice;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

// Book-Service çağrısı başarısız olunca Feign buraya düşer (Fallback = B planı).
// "cause" = asıl hatanın sebebi. Ona bakıp kullanıcıya anlamlı bir hata döneriz.
@Slf4j
@Component
public class BookClientFallbackFactory implements FallbackFactory<BookClient> {

    @Override
    public BookClient create(Throwable cause) {
        return new BookClient() {
            @Override
            public BookDto getBookById(Long id) {
                throw toException(cause, "getBookById", id);
            }

            @Override
            public BookDto decreaseStock(Long id) {
                throw toException(cause, "decreaseStock", id);
            }

            @Override
            public BookDto increaseStock(Long id) {
                throw toException(cause, "increaseStock", id);
            }
        };
    }

    private RuntimeException toException(Throwable cause, String methodName, Long bookId) {
        // İş kuralı hataları (kitap yok / stok yok) Book-Service'in ARIZASI değildir.
        // Onları değiştirmeden aynen kullanıcıya iletiyoruz.
        if (cause instanceof BookNotFoundException ex) {
            return ex;
        }
        if (cause instanceof OutOfStockException ex) {
            return ex;
        }

        // Devre kesici AÇIK: Book-Service son zamanlarda çok hata verdi, bir süre hiç denemiyoruz.
        if (cause instanceof CallNotPermittedException) {
            log.warn("Devre kesici açık, Book-Service çağrılmadı. metot={}, bookId={}", methodName, bookId);
            return new BookServiceUnavailableException(
                    "Kitap servisi şu an yanıt vermiyor, lütfen biraz sonra tekrar deneyin.");
        }

        // Diğer her şey: Book-Service kapalı, zaman aşımı, 5xx vb.
        log.error("Book-Service çağrısı başarısız. metot={}, bookId={}, sebep={}", methodName, bookId, cause.toString());
        return new BookServiceUnavailableException(
                "Kitap servisine şu an ulaşılamıyor, lütfen biraz sonra tekrar deneyin.");
    }
}
