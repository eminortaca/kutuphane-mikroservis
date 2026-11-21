package com.kutuphane.apigateway.security;

// Doğrulanmış JWT token içinden çözülen kullanıcı kimliği ve rolü
public record JwtUser(String username, String role, Long memberId) {
}
