package dev.eyaz.lib.of.alex.server.gateway.integration;


import dev.eyaz.lib.of.alex.server.gateway.filter.routing.RouteConfig;
import dev.eyaz.lib.of.alex.server.gateway.filter.routing.RoutingProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@TestConfiguration
class TestRoutingConfig {

    @Bean
    @Primary
    RoutingProperties testRoutingProperties() {
        String wireMockUrl = "http://localhost:" + GatewayIntegrationTest.wireMockServer.port();

        return new RoutingProperties(List.of(
                new RouteConfig(
                        "/register",
                        "service-auth",
                        wireMockUrl + "/api/v1/auth"
                )
        ));
    }
}
