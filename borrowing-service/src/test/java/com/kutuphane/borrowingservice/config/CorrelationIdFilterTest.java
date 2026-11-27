package com.kutuphane.borrowingservice.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void doFilter_shouldPutCorrelationIdInMdcAndResponseThenClearAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/borrowings");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "cid-test-99");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> mdcInsideChain.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertEquals("cid-test-99", mdcInsideChain.get());
        assertEquals("cid-test-99", response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void doFilter_whenHeaderMissing_shouldGenerateCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/borrowings");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String generated = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(generated);
        assertFalse(generated.isBlank());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
