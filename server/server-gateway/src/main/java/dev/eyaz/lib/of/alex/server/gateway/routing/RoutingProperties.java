package dev.eyaz.lib.of.alex.server.gateway.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gateway.routing")
public record RoutingProperties(
        List<RouteConfig> routes
) {
}
