package nz.fox.craig.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.slf4j.LoggerFactory;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpRequestLoggingFilterTest {

    private final HttpRequestLoggingFilter filter =
            new HttpRequestLoggingFilter();

    private Logger logger;
    private ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender;
    private boolean originalAdditive;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(
                HttpRequestLoggingFilter.class);

        originalAdditive = logger.isAdditive();

        appender = new ListAppender<>();
        appender.start();

        logger.addAppender(appender);
        logger.setAdditive(false);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setAdditive(originalAdditive);
        appender.stop();
    }

    @Test
    void logsSuccessfulRequest() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/orders");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);

        assertThat(appender.list)
                .hasSize(1);

        var event = appender.list.getFirst();

        assertThat(event.getLevel())
                .isEqualTo(Level.INFO);

        assertThat(event.getFormattedMessage())
                .contains("HTTP request completed")
                .contains("GET")
                .contains("/api/orders")
                .contains("200")
                .contains("ms");
    }

    @Test
    void logsRequestWhenFilterChainThrows() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/api/orders");
    
        MockHttpServletResponse response =
                new MockHttpServletResponse();
    
        FilterChain chain = mock(FilterChain.class);
    
        ServletException exception =
                new ServletException("Request failed");
    
        doAnswer(invocation -> {
            response.setStatus(500);
            throw exception;
        }).when(chain).doFilter(request, response);
    
        assertThatThrownBy(() ->
                filter.doFilter(request, response, chain))
                .isSameAs(exception);
    
        verify(chain).doFilter(request, response);
    
        assertThat(appender.list)
                .hasSize(1);
    
        var event = appender.list.getFirst();
    
        assertThat(event.getLevel())
                .isEqualTo(Level.INFO);
    
        assertThat(event.getFormattedMessage())
                .contains("HTTP request completed")
                .contains("POST")
                .contains("/api/orders")
                .contains("500")
                .contains("ms");
    }
}
