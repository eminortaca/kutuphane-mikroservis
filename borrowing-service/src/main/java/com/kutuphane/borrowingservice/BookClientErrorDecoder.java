package com.kutuphane.borrowingservice;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

// Feign, Book-Service'ten 4xx/5xx cevap gelince varsayılan olarak "FeignException" fırlatır
// ve bu da bizde 500 olarak görünürdü. Bu sınıf HTTP kodunu bizim anlamlı hatalarımıza çevirir:
//   404 -> BookNotFoundException
//   409 -> OutOfStockException
//   diğerleri -> BookServiceUnavailableException
@Component
@RequiredArgsConstructor
public class BookClientErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        // Book-Service'in gönderdiği Türkçe hata mesajını ({"message": "..."}) okuyup aynen aktarıyoruz
        String message = readMessage(response);

        return switch (response.status()) {
            case 404 -> new BookNotFoundException(message != null ? message : "Kitap bulunamadı!");
            case 409 -> new OutOfStockException(message != null ? message : "Kitabın stoğu kalmamış!");
            default -> new BookServiceUnavailableException(
                    "Book-Service hata döndürdü (HTTP " + response.status() + ")");
        };
    }

    private String readMessage(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (InputStream body = response.body().asInputStream()) {
            String message = objectMapper.readTree(body).path("message").asString();
            return message.isBlank() ? null : message;
        } catch (Exception e) {
            return null; // Gövde okunamazsa varsayılan mesajı kullanırız
        }
    }
}
