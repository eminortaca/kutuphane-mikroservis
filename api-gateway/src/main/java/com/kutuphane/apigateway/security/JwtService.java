package com.kutuphane.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

// Ek harici kütüphane gerektirmeden standart JDK (HmacSHA256 + Base64) ile
// gerçek RFC 7519 uyumlu HS256 JWT üretir ve doğrular.
@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secretBytes;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-seconds:3600}") long expirationSeconds,
            ObjectMapper objectMapper) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
        this.objectMapper = objectMapper;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String generateToken(String username, String role, Long memberId) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("role", role);
        if (memberId != null) {
            payload.put("memberId", memberId);
        }
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);

        String encodedHeader = encodeBase64Url(writeJsonBytes(header));
        String encodedPayload = encodeBase64Url(writeJsonBytes(payload));
        String signingInput = encodedHeader + "." + encodedPayload;
        String encodedSignature = encodeBase64Url(sign(signingInput));

        return signingInput + "." + encodedSignature;
    }

    public JwtUser parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token bulunamadı!");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("Geçersiz token formatı!");
        }

        byte[] actualSignature;
        try {
            actualSignature = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("Geçersiz token imzası!");
        }

        String signingInput = parts[0] + "." + parts[1];
        byte[] expectedSignature = sign(signingInput);
        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw new InvalidTokenException("Token imzası geçersiz!");
        }

        Map<String, Object> claims;
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            claims = objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new InvalidTokenException("Token içeriği okunamadı!");
        }

        Object expObj = claims.get("exp");
        if (!(expObj instanceof Number expNumber)) {
            throw new InvalidTokenException("Token bitiş süresi eksik!");
        }
        if (Instant.now().getEpochSecond() >= expNumber.longValue()) {
            throw new InvalidTokenException("Token süresi dolmuş!");
        }

        String username = claims.get("sub") != null ? claims.get("sub").toString() : null;
        String role = claims.get("role") != null ? claims.get("role").toString() : null;
        if (username == null || username.isBlank() || role == null || role.isBlank()) {
            throw new InvalidTokenException("Token kullanıcı bilgisi eksik!");
        }

        Object memberIdObj = claims.get("memberId");
        Long memberId = (memberIdObj instanceof Number num) ? num.longValue() : null;

        return new JwtUser(username, role, memberId);
    }

    private byte[] writeJsonBytes(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception ex) {
            throw new IllegalStateException("JWT JSON oluşturulamadı", ex);
        }
    }

    private byte[] sign(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT imzalanamadı", ex);
        }
    }

    private String encodeBase64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
