package dev.eyaz.lib.of.alex.server.gateway.filter.routing;

import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.CircuitOpenException;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.DownstreamUnavailableException;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.GatewayException;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.NoRouteFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Last filter in the chain (after JwtValidationWebFilter and
 * RateLimitingWebFilter): resolves the target downstream service from the
 * path (FR1), protects the call with that service's circuit breaker
 * (ADR-009/ADR-004: count-based opening, configured per-service in
 * application.yml), and forwards the request/response as a reactive proxy.
 *
 * ADR-012: circuit breaker check happens as part of the single downstream
 * call below because there is currently only ONE static instance per
 * service — service-level and instance-level circuit breaking are
 * equivalent in that state. Deliberately NOT building a load balancer or
 * per-instance circuit breaker yet: ADR-012 explicitly documents that as
 * unnecessary complexity while only one instance exists, and specifies the
 * exact future change (load balancer instance selection BEFORE the circuit
 * breaker check, one breaker per instance instead of one per service) to
 * apply only once a service is actually deployed with multiple instances.
 *
 * WHY CircuitBreakerOperator INSTEAD OF MANUAL STATE CHECK: calling
 * circuitBreaker.getState() to decide whether to proceed, THEN making the
 * call, would be a check-then-act race — the same bug class already seen
 * in this project (service-auth's refresh token collision, the rate
 * limiter's token bucket). CircuitBreakerOperator.of(cb) instead wraps the
 * call itself: Resilience4j atomically decides whether to permit it, and
 * automatically records the outcome (success/failure/rejected) against the
 * breaker's own state — no separate check step exists to race against.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RoutingWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RoutingWebFilter.class);

    private final RoutingProperties routingProperties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final WebClient webClient;

    public RoutingWebFilter(
            RoutingProperties routingProperties,
            CircuitBreakerRegistry circuitBreakerRegistry,
            WebClient.Builder webClientBuilder
    ) {
        this.routingProperties = routingProperties;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        // No baseUrl here on purpose: routes target different downstream
        // services with different base URLs (resolved per-request below),
        // unlike the ServiceAuth-specific WebClient bean in WebClientConfig.
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        RouteConfig route = resolveRoute(path);
        if (route == null) {
            return Mono.error(new NoRouteFoundException(path));
        }
        log.info("Routing request: path={}, service={}, target={}", path, route.serviceName(), route.baseUrl() + path + queryStringOrEmpty(exchange));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(route.serviceName());

        Mono<ClientResponse> downstreamCall = webClient
                .method(exchange.getRequest().getMethod())
                .uri(route.baseUrl() + path + queryStringOrEmpty(exchange))
                .headers(headers -> headers.addAll(exchange.getRequest().getHeaders()))
                .body(BodyInserters.fromDataBuffers(exchange.getRequest().getBody()))
                .exchangeToMono(Mono::just);

        log.info("downstream call : " + downstreamCall.toString());

        return downstreamCall
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .flatMap(clientResponse -> forwardResponse(exchange, clientResponse))
                .onErrorMap(CallNotPermittedException.class,
                        ex -> new CircuitOpenException(route.serviceName()))
                .onErrorMap(ex -> !(ex instanceof GatewayException),
                        ex -> new DownstreamUnavailableException(route.serviceName(), ex));
    }

    private Mono<Void> forwardResponse(ServerWebExchange exchange, ClientResponse clientResponse) {
        exchange.getResponse().setStatusCode(clientResponse.statusCode());
        exchange.getResponse().getHeaders().addAll(clientResponse.headers().asHttpHeaders());
        return exchange.getResponse().writeWith(clientResponse.bodyToFlux(DataBuffer.class));
    }

    private RouteConfig resolveRoute(String path) {
        return routingProperties.routes().stream()
                .filter(route -> path.startsWith(route.pathPrefix()))
                .findFirst()
                .orElse(null);
    }

    private String queryStringOrEmpty(ServerWebExchange exchange) {
        String query = exchange.getRequest().getURI().getRawQuery();
        return query != null ? "?" + query : "";
    }
}
