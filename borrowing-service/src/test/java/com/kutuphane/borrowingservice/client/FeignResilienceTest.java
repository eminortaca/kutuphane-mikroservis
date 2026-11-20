package com.kutuphane.borrowingservice.client;

import com.kutuphane.borrowingservice.exception.BookNotFoundException;
import com.kutuphane.borrowingservice.exception.BookServiceUnavailableException;
import com.kutuphane.borrowingservice.exception.MemberNotFoundException;
import com.kutuphane.borrowingservice.exception.MemberServiceUnavailableException;
import com.kutuphane.borrowingservice.exception.OutOfStockException;
import feign.Request;
import feign.Response;
import feign.Util;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeignResilienceTest {

    private final BookClientErrorDecoder errorDecoder = new BookClientErrorDecoder(new ObjectMapper());
    private final BookClientFallbackFactory bookFallbackFactory = new BookClientFallbackFactory();
    private final MemberClientFallbackFactory memberFallbackFactory = new MemberClientFallbackFactory();

    @Test
    void errorDecoder_whenBookClientReturns404Or409_shouldReturnDomainExceptions() {
        Response notFound = response(404, "{\"message\":\"Kitap bulunamadı! ID: 5\"}");
        Response outOfStock = response(409, "{\"message\":\"Stok kalmamış!\"}");

        Exception ex404 = errorDecoder.decode("BookClient#decreaseStock(Long)", notFound);
        Exception ex409 = errorDecoder.decode("BookClient#decreaseStock(Long)", outOfStock);

        assertInstanceOf(BookNotFoundException.class, ex404);
        assertEquals("Kitap bulunamadı! ID: 5", ex404.getMessage());
        assertInstanceOf(OutOfStockException.class, ex409);
        assertEquals("Stok kalmamış!", ex409.getMessage());
    }

    @Test
    void errorDecoder_whenMemberClientReturns404_shouldReturnMemberNotFoundException() {
        Response notFound = response(404, "{\"message\":\"Üye bulunamadı! ID: 7\"}");

        Exception ex = errorDecoder.decode("MemberClient#getMemberById(Long)", notFound);

        assertInstanceOf(MemberNotFoundException.class, ex);
        assertEquals("Üye bulunamadı! ID: 7", ex.getMessage());
    }

    @Test
    void bookFallback_shouldPropagateBusinessExceptionsAndWrapCircuitOpenAs503() {
        OutOfStockException stockEx = new OutOfStockException("Stok bitti");
        BookClient passthroughClient = bookFallbackFactory.create(stockEx);
        assertSame(stockEx, assertThrows(OutOfStockException.class, () -> passthroughClient.decreaseStock(1L)));

        CallNotPermittedException openEx = CallNotPermittedException
                .createCallNotPermittedException(CircuitBreaker.ofDefaults("book-service"));
        BookClient unavailableClient = bookFallbackFactory.create(openEx);
        assertThrows(BookServiceUnavailableException.class, () -> unavailableClient.decreaseStock(1L));
    }

    @Test
    void memberFallback_shouldPropagateNotFoundAndWrapCircuitOpenAs503() {
        MemberNotFoundException notFoundEx = new MemberNotFoundException("Üye bulunamadı!");
        MemberClient passthroughClient = memberFallbackFactory.create(notFoundEx);
        assertSame(notFoundEx, assertThrows(MemberNotFoundException.class, () -> passthroughClient.getMemberById(1L)));

        CallNotPermittedException openEx = CallNotPermittedException
                .createCallNotPermittedException(CircuitBreaker.ofDefaults("member-service"));
        MemberClient unavailableClient = memberFallbackFactory.create(openEx);
        assertThrows(MemberServiceUnavailableException.class, () -> unavailableClient.getMemberById(1L));
    }

    private Response response(int status, String jsonBody) {
        Request request = Request.create(Request.HttpMethod.GET, "/test", Map.of(), null, Util.UTF_8, null);
        return Response.builder()
                .status(status)
                .reason("Test")
                .request(request)
                .headers(Map.of())
                .body(jsonBody, StandardCharsets.UTF_8)
                .build();
    }
}
