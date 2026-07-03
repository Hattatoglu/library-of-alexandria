package dev.eyaz.lib.of.alex.server.gateway.key;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gateway.auth")
public record GatewayAuthProperties(
        String serviceAuthBaseUrl,
        Duration publicKeyCacheTtl
) {
}
