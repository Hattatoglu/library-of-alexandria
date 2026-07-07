package dev.eyaz.lib.of.alex.server.gateway.unit.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyaz.lib.of.alex.server.gateway.actuator.GatewayMetrics;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.JwtValidationException;
import dev.eyaz.lib.of.alex.server.gateway.exception.handler.GlobalErrorWebExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GlobalErrorWebExceptionHandlerTest {

    @Test
    void mapsGatewayExceptionToItsOwnStatusAndRecordsMetric() {
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        GlobalErrorWebExceptionHandler handler = new GlobalErrorWebExceptionHandler(new ObjectMapper(), gatewayMetrics);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books").build());

        StepVerifier.create(handler.handle(exchange, new JwtValidationException("token_expired")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(gatewayMetrics).recordError("token_expired", 401);
    }

    @Test
    void mapsUnexpectedExceptionTo500WithoutLeakingDetails() {
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        GlobalErrorWebExceptionHandler handler = new GlobalErrorWebExceptionHandler(new ObjectMapper(), gatewayMetrics);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books").build());

        StepVerifier.create(handler.handle(exchange, new RuntimeException("some internal SQL detail")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(gatewayMetrics).recordError("internal_error", 500);
    }
}