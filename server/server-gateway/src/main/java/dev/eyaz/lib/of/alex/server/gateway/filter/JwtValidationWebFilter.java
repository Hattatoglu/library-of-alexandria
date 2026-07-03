package dev.eyaz.lib.of.alex.server.gateway.filter;

import dev.eyaz.lib.of.alex.server.gateway.actuator.GatewayMetrics;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.JwtValidationException;
import dev.eyaz.lib.of.alex.server.gateway.jwt.JwtValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * First content filter in the chain (after CorrelationIdWebFilter, which
 * runs at HIGHEST_PRECEDENCE - 10 so every log line here already carries a
 * correlation ID). Implements GF-02.
 *
 * Deep dive #1 finding still applies: no .block() anywhere in this chain,
 * and the failure path must write the response AND call setComplete()
 * without ever calling chain.filter() — see JwtValidationWebFilterTest.
 *
 * Note: PublicKeyUnavailableException (raised by JwtValidator when
 * ServiceAuth itself can't be reached) is NOT a JwtValidationException, so
 * it is intentionally NOT caught by the onErrorResume below — it propagates
 * to GlobalErrorWebExceptionHandler and is answered with 503, not 401.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtValidationWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationWebFilter.class);

    private final JwtValidator jwtValidator;
    private final GatewayMetrics gatewayMetrics;

    public JwtValidationWebFilter(JwtValidator jwtValidator, GatewayMetrics gatewayMetrics) {
        this.jwtValidator = jwtValidator;
        this.gatewayMetrics = gatewayMetrics;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("access_token");

        if (cookie == null) {
            return unauthorized(exchange, "missing_token");
        }

        return jwtValidator.validate(cookie.getValue())
                .flatMap(user -> {
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(builder -> builder
                                    .header("X-User-Id", user.userId())
                                    .header("X-User-Role", String.join(",", user.roles())))
                            .build();
                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(JwtValidationException.class,
                        ex -> unauthorized(exchange, ex.errorCode()));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reasonCode) {
        log.debug("Access token rejected: reason={}, path={}", reasonCode, exchange.getRequest().getPath());
        gatewayMetrics.recordAuthFailure(reasonCode);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", reasonCode);
        return exchange.getResponse().setComplete();
    }
}
