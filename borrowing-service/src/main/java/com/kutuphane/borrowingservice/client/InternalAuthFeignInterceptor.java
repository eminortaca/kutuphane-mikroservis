package com.kutuphane.borrowingservice.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Feign ile başka servislere giden tüm isteklere otomatik olarak iç servis gizli anahtarını ekler.
// Böylece Book-Service üzerindeki /internal/books/** uç noktaları dış erişime kapalı kalırken
// Borrowing-Service güvenle stok güncelleyebilir.
@Component
public class InternalAuthFeignInterceptor implements RequestInterceptor {

    @Value("${internal.api.secret:kutuphane-internal-secret-2026}")
    private String internalSecret;

    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Internal-Secret", internalSecret);
    }
}
