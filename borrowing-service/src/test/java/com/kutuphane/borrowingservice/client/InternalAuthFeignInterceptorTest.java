package com.kutuphane.borrowingservice.client;

import com.kutuphane.borrowingservice.config.CorrelationIdFilter;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class InternalAuthFeignInterceptorTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void apply_shouldAttachInternalSecretAndCorrelationIdFromMdc() {
        InternalAuthFeignInterceptor interceptor = new InternalAuthFeignInterceptor();
        ReflectionTestUtils.setField(interceptor, "internalSecret", "gizli-test-anahtari");
        MDC.put(CorrelationIdFilter.MDC_KEY, "trace-abc-123");

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertTrue(template.headers().get("X-Internal-Secret").contains("gizli-test-anahtari"));
        assertTrue(template.headers().get(CorrelationIdFilter.CORRELATION_ID_HEADER).contains("trace-abc-123"));
    }
}
