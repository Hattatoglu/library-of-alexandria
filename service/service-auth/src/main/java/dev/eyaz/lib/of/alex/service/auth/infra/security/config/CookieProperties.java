package dev.eyaz.lib.of.alex.service.auth.infra.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cookie")
public record CookieProperties(
        String domain,
        boolean secure,
        String sameSite
) {
    public static final String ACCESS_TOKEN_NAME  = "access_token";
    public static final String REFRESH_TOKEN_NAME = "refresh_token";
}
