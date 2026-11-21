package com.kutuphane.apigateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// API Gateway üzerinde sade kimlik doğrulama uç noktaları (/api/auth/login, /api/auth/me)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private record DemoUser(String username, String password, String role, Long memberId) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(
            String token,
            String tokenType,
            long expiresIn,
            String username,
            String role,
            Long memberId
    ) {
    }

    private static final Map<String, DemoUser> DEMO_USERS = Map.of(
            "admin", new DemoUser("admin", "admin123", "ADMIN", null),
            "librarian", new DemoUser("librarian", "lib123", "LIBRARIAN", null),
            "member1", new DemoUser("member1", "member123", "MEMBER", 1L),
            "member2", new DemoUser("member2", "member123", "MEMBER", 2L)
    );

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return unauthorized("Kullanıcı adı ve şifre zorunludur!");
        }

        DemoUser user = DEMO_USERS.get(request.username().trim());
        if (user == null || !user.password().equals(request.password())) {
            return unauthorized("Kullanıcı adı veya şifre hatalı!");
        }

        String token = jwtService.generateToken(user.username(), user.role(), user.memberId());
        return ResponseEntity.ok(new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.username(),
                user.role(),
                user.memberId()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized("Geçerli bir Bearer token gönderilmelidir!");
        }

        try {
            String token = authHeader.substring(7).trim();
            JwtUser user = jwtService.parseAndValidate(token);
            return ResponseEntity.ok(user);
        } catch (InvalidTokenException ex) {
            return unauthorized(ex.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> unauthorized(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
