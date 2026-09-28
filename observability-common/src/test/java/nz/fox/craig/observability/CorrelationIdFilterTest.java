package nz.fox.craig.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();
    private static final String CORRELATION_ID = "17eec180-d060-4cf6-977e-5006a8c24a90";

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preservesExistingCorrelationId() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, CORRELATION_ID);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(CorrelationId.HEADER))
                .isEqualTo(CORRELATION_ID);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        String correlationId = response.getHeader(CorrelationId.HEADER);

        assertThat(correlationId)
                .isNotNull()
                .isNotBlank();

        assertThat(correlationId)
                .matches("[0-9a-fA-F-]{36}");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void removesCorrelationIdFromMdcAfterRequest() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, CORRELATION_ID);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(MDC.get(CorrelationId.MDC_KEY))
                .isNull();
    }

    @Test
    void makesCorrelationIdAvailableInMdcDuringRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, CORRELATION_ID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) ->
                assertThat(MDC.get(CorrelationId.MDC_KEY))
                        .isEqualTo(CORRELATION_ID);

        filter.doFilter(request, response, filterChain);
    }
}
