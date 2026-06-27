package dev.eyaz.lib.of.alex.service.auth.infra.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String privateKeyPath,
        String publicKeyPath,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs
) {}
