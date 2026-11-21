package com.kutuphane.apigateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// API Gateway üzerinden geçen tüm isteklerde merkezi JWT doğrulaması ve rol kontrolü yapar.
@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthGatewayFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return -100; // Yönlendirmeden önce çalışsın
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();

        // 1. CORS ön-kontrol (preflight), auth ve actuator uç noktaları serbest
        if (HttpMethod.OPTIONS.equals(method)
                || path.startsWith("/api/auth")
                || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        // 2. Katalog okuma (GET /api/books ve GET /api/books/{id}) herkese açık
        if (HttpMethod.GET.equals(method)
                && (path.equals("/api/books") || path.startsWith("/api/books/"))) {
            return chain.filter(exchange);
        }

        // 3. Diğer tüm /api/** istekleri için geçerli JWT zorunlu
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Bu işlem için giriş yapmalısınız (Bearer token eksik)!");
        }

        JwtUser user;
        try {
            String token = authHeader.substring(7).trim();
            user = jwtService.parseAndValidate(token);
        } catch (InvalidTokenException ex) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage());
        }

        // 4. Rol bazlı yetkilendirme kontrolü
        String forbiddenReason = checkAuthorizationError(method, path, request.getQueryParams(), user);
        if (forbiddenReason != null) {
            return writeError(exchange, HttpStatus.FORBIDDEN, forbiddenReason);
        }

        // 5. İstemcinin göndermiş olabileceği sahte kimlik başlıklarını temizle ve doğrulanmış kimliği ekle
        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.remove("X-Auth-User");
                    headers.remove("X-Auth-Role");
                    headers.remove("X-Auth-Member-Id");
                    headers.set("X-Auth-User", user.username());
                    headers.set("X-Auth-Role", user.role());
                    if (user.memberId() != null) {
                        headers.set("X-Auth-Member-Id", String.valueOf(user.memberId()));
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String checkAuthorizationError(
            HttpMethod method,
            String path,
            MultiValueMap<String, String> queryParams,
            JwtUser user) {
        String role = user.role();
        if ("ADMIN".equalsIgnoreCase(role) || "LIBRARIAN".equalsIgnoreCase(role)) {
            return null;
        }

        if (!"MEMBER".equalsIgnoreCase(role)) {
            return "Bu işlem için yetkiniz yok!";
        }

        // MEMBER: Kitap ekleyemez, güncelleyemez, silemez
        if (path.equals("/api/books") || path.startsWith("/api/books/")) {
            return "Kitap envanterini yalnızca ADMIN veya LIBRARIAN değiştirebilir!";
        }

        // MEMBER: Üye işlemlerinde sadece kendi profilini (GET /api/members/{kendiId}) görebilir
        if (path.equals("/api/members") || path.startsWith("/api/members/")) {
            if (HttpMethod.GET.equals(method)
                    && user.memberId() != null
                    && path.equals("/api/members/" + user.memberId())) {
                return null;
            }
            return "Üye yönetimi yalnızca ADMIN veya LIBRARIAN yetkisindedir!";
        }

        // MEMBER: Ödünç alırken sadece kendi memberId'si adına alabilir
        if (path.equals("/api/borrowings") || path.startsWith("/api/borrowings/")) {
            if (HttpMethod.POST.equals(method) && path.equals("/api/borrowings")) {
                String requestedMemberId = queryParams.getFirst("memberId");
                if (user.memberId() == null
                        || requestedMemberId == null
                        || !String.valueOf(user.memberId()).equals(requestedMemberId.trim())) {
                    return "Üyeler yalnızca kendi memberId'leri (" + user.memberId() + ") adına kitap ödünç alabilir!";
                }
            }
            return null;
        }

        return "Bu işlem için yetkiniz yok!";
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception ex) {
            bytes = ("{\"status\":" + status.value() + ",\"message\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
