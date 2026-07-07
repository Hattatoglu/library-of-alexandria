package dev.eyaz.lib.of.alex.server.gateway.unit.filter.jwt;

import dev.eyaz.lib.of.alex.server.gateway.actuator.GatewayMetrics;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.JwtValidationException;
import dev.eyaz.lib.of.alex.server.gateway.filter.jwt.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtValidationWebFilterTest {

    private final PublicPathProperties publicPathProperties = new PublicPathProperties(List.of(
            new PublicPath("/register"),
            new PublicPath("/login"),
            new PublicPath("/refresh"),
            new PublicPath("/logout")
    ));

    @BeforeAll
    static void installBlockHound() {
        BlockHound.install();
    }

    @Test
    void bypassesValidationForPublicPath() {
        JwtValidator validator = mock(JwtValidator.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        JwtValidationWebFilter filter = new JwtValidationWebFilter(validator, gatewayMetrics, publicPathProperties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/login").build());

        // No cookie, JwtValidator never invoked — but the filter must still
        // delegate to chain.filter() since /login is public.
        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .verifyComplete();

        verifyNoInteractions(validator);
        verifyNoInteractions(gatewayMetrics);
    }

    @Test
    void doesNotTreatPublicPathAsPrefixForUnrelatedPaths() {
        // Regression test for the startsWith boundary bug: "/loginextra"
        // must NOT be treated as public just because it starts with "/login".
        JwtValidator validator = mock(JwtValidator.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        JwtValidationWebFilter filter = new JwtValidationWebFilter(validator, gatewayMetrics, publicPathProperties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/loginextra").build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> {
                    throw new AssertionError("chain.filter() must not be called — /loginextra is not public and has no cookie");
                }))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Auth-Error")).isEqualTo("missing_token");
    }

    @Test
    void rejectsRequestWithMissingTokenAndRecordsMetric() {
        JwtValidator validator = mock(JwtValidator.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        JwtValidationWebFilter filter = new JwtValidationWebFilter(validator, gatewayMetrics, publicPathProperties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books").build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Auth-Error")).isEqualTo("missing_token");
        verify(gatewayMetrics).recordAuthFailure("missing_token");
    }

    @Test
    void deniesRequestOnInvalidTokenWithoutCallingChainAndRecordsMetric() {
        JwtValidator validator = mock(JwtValidator.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        when(validator.validate("bad-token"))
                .thenReturn(Mono.error(new dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.JwtValidationException("invalid_token")));

        JwtValidationWebFilter filter = new JwtValidationWebFilter(validator, gatewayMetrics, publicPathProperties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books")
                        .cookie(new HttpCookie("access_token", "bad-token"))
                        .build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> {
                    throw new AssertionError("chain.filter() must not be called on invalid token");
                }))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Auth-Error")).isEqualTo("invalid_token");
        verify(gatewayMetrics).recordAuthFailure("invalid_token");
    }

    @Test
    void forwardsRequestWithIdentityHeadersOnValidToken() {
        JwtValidator validator = mock(JwtValidator.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        AuthenticatedUser user = new AuthenticatedUser("user-123", "emre", List.of("ADMIN", "USER"));
        when(validator.validate("good-token")).thenReturn(Mono.just(user));

        JwtValidationWebFilter filter = new JwtValidationWebFilter(validator, gatewayMetrics, publicPathProperties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books")
                        .cookie(new HttpCookie("access_token", "good-token"))
                        .build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> {
                    assertThat(chainExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("user-123");
                    assertThat(chainExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ADMIN,USER");
                    return Mono.empty();
                }))
                .verifyComplete();

        verify(gatewayMetrics, never()).recordAuthFailure(anyString());
    }

    @Test
    void rejectsRequestWithMissingToken() {
        JwtValidator validator = mock(JwtValidator.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        JwtValidationWebFilter filter = new JwtValidationWebFilter(validator, gatewayMetrics, publicPathProperties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books").build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Auth-Error"))
                .isEqualTo("missing_token");
    }

    @Test
    void deniesRequestOnInvalidSignatureWithoutCallingChain() {
        JwtValidator validator = mock(JwtValidator.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        when(validator.validate("bad-token"))
                .thenReturn(Mono.error(new JwtValidationException("invalid_signature")));

        JwtValidationWebFilter filter = new JwtValidationWebFilter(validator, gatewayMetrics, publicPathProperties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books")
                        .cookie(new org.springframework.http.HttpCookie("access_token", "bad-token"))
                        .build());

        // chain.filter() intentionally throws if invoked — proving the
        // early-return path never reaches downstream filters on failure.
        StepVerifier.create(filter.filter(exchange, chainExchange -> {
                    throw new AssertionError("chain.filter() must not be called on invalid token");
                }))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}