package nz.fox.craig.order.observability;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import nz.fox.craig.observability.CorrelationId;
import nz.fox.craig.observability.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.context.annotation.Bean;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


class CorrelationIdFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .addFilters(new CorrelationIdFilter())
                .build();
    }


    @Test
    void preservesCorrelationIdFromIncomingRequest() throws Exception {
        String correlationId = "test-correlation-123";

        mockMvc.perform(
                get("/test")
                        .header(CorrelationId.HEADER, correlationId))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                CorrelationId.HEADER,
                                correlationId));
    }

    @Test
    void generatesCorrelationIdWhenIncomingRequestDoesNotContainOne()
            throws Exception {

        MvcResult result = mockMvc.perform(
            get("/test")
                    .with(user("test-user")))
            .andExpect(status().isOk())
            .andReturn();
    

        String correlationId = result.getResponse()
                .getHeader(CorrelationId.HEADER);

        assertThat(correlationId)
                .isNotNull()
                .isNotBlank()
                .matches("[0-9a-fA-F-]{36}");
    }

    @TestConfiguration
    static class TestControllerConfig {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/test")
        String test() {
            return "OK";
        }
    }
}