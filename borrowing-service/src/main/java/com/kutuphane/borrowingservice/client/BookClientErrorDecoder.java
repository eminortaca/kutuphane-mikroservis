package com.kutuphane.borrowingservice.client;

import com.kutuphane.borrowingservice.exception.BookNotFoundException;
import com.kutuphane.borrowingservice.exception.BookServiceUnavailableException;
import com.kutuphane.borrowingservice.exception.MemberNotFoundException;
import com.kutuphane.borrowingservice.exception.MemberServiceUnavailableException;
import com.kutuphane.borrowingservice.exception.OutOfStockException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

// Feign, diğer servislerden 4xx/5xx cevap gelince varsayılan olarak "FeignException" fırlatır.
// Tek bir ErrorDecoder bean'i tüm Feign Client'lar için çalışır; methodKey üzerinden hangi istemci
// olduğunu ayırt ederek HTTP kodlarını kendi anlamlı istisnalarımıza çeviriyoruz.
@Component
@RequiredArgsConstructor
public class BookClientErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = readMessage(response);

        if (methodKey != null && methodKey.startsWith("MemberClient")) {
            return switch (response.status()) {
                case 404 -> new MemberNotFoundException(message != null ? message : "Üye bulunamadı!");
                default -> new MemberServiceUnavailableException(
                        "Member-Service hata döndürdü (HTTP " + response.status() + ")");
            };
        }

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
