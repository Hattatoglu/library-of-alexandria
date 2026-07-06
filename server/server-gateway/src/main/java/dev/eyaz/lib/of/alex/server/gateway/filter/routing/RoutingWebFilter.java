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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RoutingWebFilter implements WebFilter {

    private static final Logger log =
            LoggerFactory.getLogger(RoutingWebFilter.class);

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "Connection",
            "Keep-Alive",
            "Proxy-Authenticate",
            "Proxy-Authorization",
            "TE",
            "Trailer",
            "Transfer-Encoding",
            "Upgrade",
            "Content-Length"
    );

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

        /*
         * No fixed baseUrl:
         * each route dynamically targets a different downstream service.
         */
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {

        String path = exchange.getRequest()
                .getPath()
                .value();

        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        RouteConfig route = resolveRoute(path);

        if (route == null) {
            return Mono.error(new NoRouteFoundException(path));
        }

        String targetUrl =
                route.baseUrl()
                        + path
                        + queryStringOrEmpty(exchange);

        log.info(
                "Routing request: path={}, service={}, target={}",
                path,
                route.serviceName(),
                targetUrl
        );

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker(
                        route.serviceName()
                );

        return webClient
                .method(exchange.getRequest().getMethod())
                .uri(targetUrl)
                .headers(headers -> copyRequestHeaders(exchange, headers))
                .body(exchange.getRequest().getBody(), DataBuffer.class)
                .exchangeToMono(clientResponse ->
                        forwardResponse(exchange, clientResponse))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorMap(
                        CallNotPermittedException.class,
                        ex -> new CircuitOpenException(
                                route.serviceName()
                        )
                )
                .onErrorMap(
                        ex -> !(ex instanceof GatewayException),
                        ex -> new DownstreamUnavailableException(
                                route.serviceName(),
                                ex
                        )
                );
    }

    private Mono<Void> forwardResponse(
            ServerWebExchange exchange,
            ClientResponse clientResponse
    ) {

        exchange.getResponse()
                .setStatusCode(clientResponse.statusCode());

        copyResponseHeaders(clientResponse, exchange);

        /*
         * Stream downstream response body directly to the client
         * without buffering the entire payload in memory.
         */
        return exchange.getResponse()
                .writeWith(
                        clientResponse.bodyToFlux(DataBuffer.class)
                );
    }

    private void copyRequestHeaders(
            ServerWebExchange exchange,
            HttpHeaders targetHeaders
    ) {

        exchange.getRequest()
                .getHeaders()
                .forEach((name, values) -> {

                    if (isForwardableRequestHeader(name)) {
                        targetHeaders.put(name, values);
                    }
                });
    }

    private void copyResponseHeaders(
            ClientResponse clientResponse,
            ServerWebExchange exchange
    ) {

        clientResponse.headers()
                .asHttpHeaders()
                .forEach((name, values) -> {

                    if (isForwardableResponseHeader(name)) {
                        exchange.getResponse()
                                .getHeaders()
                                .put(name, values);
                    }
                });
    }

    private boolean isForwardableRequestHeader(String headerName) {

        return HOP_BY_HOP_HEADERS.stream()
                .noneMatch(h -> h.equalsIgnoreCase(headerName))
                && !headerName.equalsIgnoreCase("Host");
    }

    private boolean isForwardableResponseHeader(String headerName) {

        return HOP_BY_HOP_HEADERS.stream()
                .noneMatch(h -> h.equalsIgnoreCase(headerName));
    }

    private RouteConfig resolveRoute(String path) {

        return routingProperties.routes()
                .stream()
                .filter(route -> path.startsWith(route.pathPrefix()))
                .findFirst()
                .orElse(null);
    }

    private String queryStringOrEmpty(ServerWebExchange exchange) {

        String query =
                exchange.getRequest()
                        .getURI()
                        .getRawQuery();

        return query != null
                ? "?" + query
                : "";
    }
}
