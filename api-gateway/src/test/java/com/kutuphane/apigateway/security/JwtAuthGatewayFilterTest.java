package com.kutuphane.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthGatewayFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtService jwtService = new JwtService(
            "test-gizli-anahtar-en-az-32-karakter-uzunlugunda",
            3600,
            objectMapper);
    private final JwtAuthGatewayFilter filter = new JwtAuthGatewayFilter(jwtService, objectMapper);

    @Test
    void publicBookGet_withoutToken_shouldPassThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/books").build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, passThroughChain(forwarded)).block();

        assertNotNull(forwarded.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedEndpoint_withoutToken_shouldReturn401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/books").build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, passThroughChain(forwarded)).block();

        assertNull(forwarded.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void memberCreatingBook_shouldReturn403() {
        String memberToken = jwtService.generateToken("member1", "MEMBER", 1L);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, passThroughChain(forwarded)).block();

        assertNull(forwarded.get());
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void adminCreatingBook_shouldPassAndSetAuthHeaders() {
        String adminToken = jwtService.generateToken("admin", "ADMIN", null);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header("X-Auth-Role", "FAKE_ROLE")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, passThroughChain(forwarded)).block();

        assertNotNull(forwarded.get());
        HttpHeaders forwardedHeaders = forwarded.get().getRequest().getHeaders();
        assertEquals("admin", forwardedHeaders.getFirst("X-Auth-User"));
        assertEquals("ADMIN", forwardedHeaders.getFirst("X-Auth-Role"));
    }

    @Test
    void memberBorrowingForDifferentMemberId_shouldReturn403() {
        String memberToken = jwtService.generateToken("member1", "MEMBER", 1L);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/borrowings?bookId=1&memberId=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, passThroughChain(forwarded)).block();

        assertNull(forwarded.get());
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void memberBorrowingForSelf_shouldPassThrough() {
        String memberToken = jwtService.generateToken("member1", "MEMBER", 1L);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/borrowings?bookId=1&memberId=1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, passThroughChain(forwarded)).block();

        assertNotNull(forwarded.get());
        HttpHeaders forwardedHeaders = forwarded.get().getRequest().getHeaders();
        assertEquals("member1", forwardedHeaders.getFirst("X-Auth-User"));
        assertEquals("1", forwardedHeaders.getFirst("X-Auth-Member-Id"));
    }

    @Test
    void memberListingAllOrOtherBorrowings_shouldReturn403() {
        String memberToken = jwtService.generateToken("member1", "MEMBER", 1L);

        MockServerWebExchange allExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/borrowings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .build());
        AtomicReference<ServerWebExchange> forwardedAll = new AtomicReference<>();
        filter.filter(allExchange, passThroughChain(forwardedAll)).block();
        assertNull(forwardedAll.get());
        assertEquals(HttpStatus.FORBIDDEN, allExchange.getResponse().getStatusCode());

        MockServerWebExchange otherExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/borrowings?memberId=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .build());
        AtomicReference<ServerWebExchange> forwardedOther = new AtomicReference<>();
        filter.filter(otherExchange, passThroughChain(forwardedOther)).block();
        assertNull(forwardedOther.get());
        assertEquals(HttpStatus.FORBIDDEN, otherExchange.getResponse().getStatusCode());
    }

    @Test
    void memberListingOwnBorrowings_shouldPassThrough() {
        String memberToken = jwtService.generateToken("member1", "MEMBER", 1L);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/borrowings?memberId=1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, passThroughChain(forwarded)).block();

        assertNotNull(forwarded.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    private GatewayFilterChain passThroughChain(AtomicReference<ServerWebExchange> captured) {
        return ex -> {
            captured.set(ex);
            return Mono.empty();
        };
    }
}
