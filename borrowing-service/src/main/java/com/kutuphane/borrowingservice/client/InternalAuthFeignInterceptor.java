package com.kutuphane.borrowingservice.client;

import com.kutuphane.borrowingservice.config.CorrelationIdFilter;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Feign ile başka servislere giden tüm isteklere otomatik olarak iç servis gizli anahtarını ve
// aktif isteğin X-Correlation-Id bilgisini ekler.
@Component
public class InternalAuthFeignInterceptor implements RequestInterceptor {

    @Value("${internal.api.secret:kutuphane-internal-secret-2026}")
    private String internalSecret;

    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Internal-Secret", internalSecret);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            template.header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
        }
    }
}
