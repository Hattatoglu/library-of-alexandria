package dev.eyaz.lib.of.alex.server.gateway.unit.filter.ratelimiter;

import dev.eyaz.lib.of.alex.server.gateway.actuator.GatewayMetrics;
import dev.eyaz.lib.of.alex.server.gateway.filter.ratelimiter.RateLimitDecision;
import dev.eyaz.lib.of.alex.server.gateway.filter.ratelimiter.RateLimitProperties;
import dev.eyaz.lib.of.alex.server.gateway.filter.ratelimiter.RateLimitingWebFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitingWebFilterTest {

    private final RateLimitProperties properties = new RateLimitProperties(100, Duration.ofSeconds(60));

    @Test
    void bypassesRateLimitForActuatorPath() {
        RateLimitDecision decision = mock(RateLimitDecision.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        RateLimitingWebFilter filter = new RateLimitingWebFilter(decision, properties, gatewayMetrics);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .verifyComplete();

        verifyNoInteractions(decision);
    }

    @Test
    void allowsRequestWhenWithinLimit() {
        RateLimitDecision decision = mock(RateLimitDecision.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        when(decision.isAllowed(anyString())).thenReturn(Mono.just(true));

        RateLimitingWebFilter filter = new RateLimitingWebFilter(decision, properties, gatewayMetrics);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books")
                        .header("X-User-Id", "user-123")
                        .build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .verifyComplete();

        verify(gatewayMetrics, never()).recordRateLimitRejection();
    }

    @Test
    void rejectsRequestWhenLimitExceeded() {
        RateLimitDecision decision = mock(RateLimitDecision.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        when(decision.isAllowed(anyString())).thenReturn(Mono.just(false));

        RateLimitingWebFilter filter = new RateLimitingWebFilter(decision, properties, gatewayMetrics);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books")
                        .header("X-User-Id", "user-123")
                        .build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> {
                    throw new AssertionError("chain.filter() must not be called when rate-limited");
                }))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
        verify(gatewayMetrics).recordRateLimitRejection();
    }

    @Test
    void usesXUserIdAsKeyWhenPresent() {
        RateLimitDecision decision = mock(RateLimitDecision.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        when(decision.isAllowed(anyString())).thenReturn(Mono.just(true));

        RateLimitingWebFilter filter = new RateLimitingWebFilter(decision, properties, gatewayMetrics);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books")
                        .header("X-User-Id", "user-123")
                        .build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .verifyComplete();

        verify(decision).isAllowed("user-123");
    }
}