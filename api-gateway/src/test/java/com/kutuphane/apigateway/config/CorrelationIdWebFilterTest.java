package com.kutuphane.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdWebFilterTest {

    private final CorrelationIdWebFilter filter = new CorrelationIdWebFilter();

    @Test
    void filter_whenHeaderMissing_shouldGenerateAndPropagateToRequestAndResponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/books").build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
        String forwardedId = forwarded.get().getRequest().getHeaders().getFirst(CorrelationIdWebFilter.CORRELATION_ID_HEADER);
        String responseId = exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.CORRELATION_ID_HEADER);

        assertNotNull(forwardedId);
        assertFalse(forwardedId.isBlank());
        assertEquals(forwardedId, responseId);
    }

    @Test
    void filter_whenHeaderPresent_shouldPreserveIncomingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/books")
                        .header(CorrelationIdWebFilter.CORRELATION_ID_HEADER, "req-12345")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
        assertEquals("req-12345",
                forwarded.get().getRequest().getHeaders().getFirst(CorrelationIdWebFilter.CORRELATION_ID_HEADER));
        assertEquals("req-12345",
                exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.CORRELATION_ID_HEADER));
    }
}
