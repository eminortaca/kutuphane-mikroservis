package com.kutuphane.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerTest {

    private final JwtService jwtService = new JwtService(
            "test-gizli-anahtar-en-az-32-karakter-uzunlugunda",
            3600,
            new ObjectMapper());
    private final AuthController authController = new AuthController(jwtService);

    @Test
    void login_whenValidCredentials_shouldReturn200AndJwtToken() {
        ResponseEntity<?> response = authController.login(new AuthController.LoginRequest("admin", "admin123"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthController.LoginResponse body = assertInstanceOf(AuthController.LoginResponse.class, response.getBody());
        assertEquals("admin", body.username());
        assertEquals("ADMIN", body.role());
        assertEquals("Bearer", body.tokenType());
        assertNotNull(body.token());

        JwtUser parsed = jwtService.parseAndValidate(body.token());
        assertEquals("admin", parsed.username());
        assertEquals("ADMIN", parsed.role());
    }

    @Test
    void login_whenWrongPassword_shouldReturn401() {
        ResponseEntity<?> response = authController.login(new AuthController.LoginRequest("admin", "yanlis"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void currentUser_whenValidBearerToken_shouldReturn200WithJwtUser() {
        String token = jwtService.generateToken("member1", "MEMBER", 1L);

        ResponseEntity<?> response = authController.currentUser("Bearer " + token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JwtUser user = assertInstanceOf(JwtUser.class, response.getBody());
        assertEquals("member1", user.username());
        assertEquals("MEMBER", user.role());
        assertEquals(1L, user.memberId());
    }
}
