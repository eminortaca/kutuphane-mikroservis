package com.kutuphane.apigateway.security;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-gizli-anahtar-en-az-32-karakter-uzunlugunda";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateAndValidateToken_shouldReturnJwtUser() {
        JwtService jwtService = new JwtService(SECRET, 3600, objectMapper);

        String token = jwtService.generateToken("member1", "MEMBER", 1L);
        JwtUser user = jwtService.parseAndValidate(token);

        assertEquals("member1", user.username());
        assertEquals("MEMBER", user.role());
        assertEquals(1L, user.memberId());
    }

    @Test
    void parseAndValidate_whenTampered_shouldThrowInvalidTokenException() {
        JwtService jwtService = new JwtService(SECRET, 3600, objectMapper);
        String token = jwtService.generateToken("member1", "MEMBER", 1L);

        // İmza kısmının son karakterini boz
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(InvalidTokenException.class, () -> jwtService.parseAndValidate(tampered));
    }

    @Test
    void parseAndValidate_whenExpired_shouldThrowInvalidTokenException() {
        JwtService expiredJwtService = new JwtService(SECRET, -5, objectMapper);
        String expiredToken = expiredJwtService.generateToken("admin", "ADMIN", null);

        InvalidTokenException ex = assertThrows(
                InvalidTokenException.class,
                () -> expiredJwtService.parseAndValidate(expiredToken));
        assertTrue(ex.getMessage().contains("süresi dolmuş"));
    }
}
