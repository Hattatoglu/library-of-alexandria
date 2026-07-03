package dev.eyaz.lib.of.alex.server.gateway.filter;

import dev.eyaz.lib.of.alex.server.gateway.actuator.GatewayMetrics;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.JwtValidationException;
import dev.eyaz.lib.of.alex.server.gateway.jwt.AuthenticatedUser;
import dev.eyaz.lib.of.alex.server.gateway.jwt.JwtValidator;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DEEP DIVE FOCUS (updated for the real JwtValidationWebFilter, now
 * depending on JwtValidator + GatewayMetrics): proves two things
 * independently, still the core deep-dive question for this chain.
 *
 * 1. StepVerifier — the reactive chain resolves correctly for the success
 *    path (token valid, chain continues, identity headers set), the
 *    missing-token path, and the invalid-token path (401 written,
 *    chain.filter() never invoked).
 *
 * 2. BlockHound — no blocking call sneaks into the chain at runtime. This
 *    is the actual answer to the deep-dive question: "does this filter
 *    chain really stay non-blocking end to end?" A unit test alone can't
 *    prove that; BlockHound instruments the JVM and throws if a blocking
 *    call executes on a Reactor event-loop thread. Because JwtValidator is
 *    mocked here, this test alone does NOT cover blocking calls inside the
 *    real JJWT parsing or WebClient public-key fetch — see the note at the
 *    bottom of this file.
 */
class JwtValidationWebFilterTest {

    @BeforeAll
    static void installBlockHound() {
        BlockHound.install();
    }

    @Test
    void rejectsRequestWithMissingTokenAndRecordsMetric() {
        JwtValidator validator = mock(JwtValidator.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        JwtValidationWebFilter filter = new JwtValidationWebFilter(validator, gatewayMetrics);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books").build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Auth-Error"))
                .isEqualTo("missing_token");
        verify(gatewayMetrics).recordAuthFailure("missing_token");
    }

    @Test
    void deniesRequestOnInvalidTokenWithoutCallingChainAndRecordsMetric() {
        JwtValidator validator = mock(JwtValidator.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        when(validator.validate("bad-token"))
                .thenReturn(Mono.error(new JwtValidationException("invalid_token")));

        JwtValidationWebFilter filter = new JwtValidationWebFilter(validator, gatewayMetrics);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books")
                        .cookie(new HttpCookie("access_token", "bad-token"))
                        .build());

        // chain.filter() intentionally throws if invoked — proving the
        // early-return path never reaches downstream filters on failure.
        StepVerifier.create(filter.filter(exchange, chainExchange -> {
                    throw new AssertionError("chain.filter() must not be called on invalid token");
                }))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Auth-Error"))
                .isEqualTo("invalid_token");
        verify(gatewayMetrics).recordAuthFailure("invalid_token");
    }

    @Test
    void forwardsRequestWithIdentityHeadersOnValidToken() {
        JwtValidator validator = mock(JwtValidator.class);
        GatewayMetrics gatewayMetrics = mock(GatewayMetrics.class);
        AuthenticatedUser user = new AuthenticatedUser("user-123", "emre", List.of("ADMIN", "USER"));
        when(validator.validate("good-token")).thenReturn(Mono.just(user));

        JwtValidationWebFilter filter = new JwtValidationWebFilter(validator, gatewayMetrics);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books")
                        .cookie(new HttpCookie("access_token", "good-token"))
                        .build());

        // The chain here asserts the mutated request carries the resolved
        // identity, then completes — proving chain.filter() IS called (and
        // called with the right headers) on the success path, complementing
        // the previous test's proof that it is NOT called on failure.
        StepVerifier.create(filter.filter(exchange, chainExchange -> {
                    assertThat(chainExchange.getRequest().getHeaders().getFirst("X-User-Id"))
                            .isEqualTo("user-123");
                    assertThat(chainExchange.getRequest().getHeaders().getFirst("X-User-Role"))
                            .isEqualTo("ADMIN,USER");
                    return Mono.empty();
                }))
                .verifyComplete();

        verify(gatewayMetrics, org.mockito.Mockito.never()).recordAuthFailure(org.mockito.ArgumentMatchers.anyString());
    }

    // NOTE ON BLOCKING-CALL COVERAGE: BlockHound.install() protects every
    // test in this class, but with JwtValidator mocked, no real JJWT
    // parsing or WebClient call ever executes here — there is nothing for
    // BlockHound to catch in these three tests. Full end-to-end blocking
    // coverage (JwtValidator + PublicKeyProvider using their REAL
    // implementations, only WebClient's transport mocked) still needs an
    // integration-level test; this class only proves JwtValidationWebFilter
    // itself introduces no blocking call in its own control flow.
}
