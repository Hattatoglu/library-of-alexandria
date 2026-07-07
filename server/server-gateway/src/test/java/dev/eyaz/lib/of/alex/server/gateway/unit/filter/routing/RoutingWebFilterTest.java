package dev.eyaz.lib.of.alex.server.gateway.unit.filter.routing;

import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.CircuitOpenException;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.DownstreamUnavailableException;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.NoRouteFoundException;
import dev.eyaz.lib.of.alex.server.gateway.filter.routing.RouteConfig;
import dev.eyaz.lib.of.alex.server.gateway.filter.routing.RoutingProperties;
import dev.eyaz.lib.of.alex.server.gateway.filter.routing.RoutingWebFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingWebFilterTest {

    @Test
    void returns404WhenNoRouteMatches() {
        RoutingProperties routingProperties = new RoutingProperties(List.of());
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        RoutingWebFilter filter = new RoutingWebFilter(routingProperties, registry, WebClient.builder());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/unknown/path").build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .expectErrorMatches(ex -> ex instanceof NoRouteFoundException nrf
                        && nrf.httpStatus() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void returns503WhenCircuitIsOpen() {
        RouteConfig route = new RouteConfig("/catalog", "catalog", "http://unused");
        RoutingProperties routingProperties = new RoutingProperties(List.of(route));

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = registry.circuitBreaker("catalog");
        circuitBreaker.transitionToOpenState(); // force OPEN before the call

        RoutingWebFilter filter = new RoutingWebFilter(routingProperties, registry, WebClient.builder());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books").build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .expectErrorMatches(ex -> ex instanceof CircuitOpenException coe
                        && coe.httpStatus() == HttpStatus.SERVICE_UNAVAILABLE)
                .verify();
    }

    @Test
    void returns502WhenDownstreamCallFails() {
        RouteConfig route = new RouteConfig("/catalog", "catalog-down", "http://unreachable-host");
        RoutingProperties routingProperties = new RoutingProperties(List.of(route));
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

        WebClient.Builder failingBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new ConnectException("Connection refused")));

        RoutingWebFilter filter = new RoutingWebFilter(routingProperties, registry, failingBuilder);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/catalog/books").build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .expectErrorMatches(ex -> ex instanceof DownstreamUnavailableException due
                        && due.httpStatus() == HttpStatus.BAD_GATEWAY)
                .verify();
    }

    @Test
    void bypassesRoutingForActuatorPath() {
        RoutingProperties routingProperties = new RoutingProperties(List.of());
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        RoutingWebFilter filter = new RoutingWebFilter(routingProperties, registry, WebClient.builder());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.filter(exchange, chainExchange -> Mono.empty()))
                .verifyComplete();
    }
}