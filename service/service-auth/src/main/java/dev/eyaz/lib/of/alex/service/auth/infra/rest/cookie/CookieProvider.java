package dev.eyaz.lib.of.alex.service.auth.infra.rest.cookie;

import dev.eyaz.lib.of.alex.service.auth.infra.security.config.CookieProperties;
import dev.eyaz.lib.of.alex.service.auth.infra.security.config.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieProvider {

    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    public CookieProvider(JwtProperties jwtProperties, CookieProperties cookieProperties) {
        this.jwtProperties = jwtProperties;
        this.cookieProperties = cookieProperties;
    }

    public void httpOnlyRefreshAndAccessTokenProvider(
            HttpServletResponse response,
            String accessToken,
            String refreshToken) {

        long accessMaxAge  = jwtProperties.accessTokenExpirationMs() / 1000;
        long refreshMaxAge = jwtProperties.refreshTokenExpirationMs() / 1000;
        ResponseCookie access = buildCookie(CookieProperties.ACCESS_TOKEN_NAME, accessToken, accessMaxAge);
        response.addHeader("Set-Cookie", access.toString());

        ResponseCookie refresh = buildCookie(CookieProperties.REFRESH_TOKEN_NAME, refreshToken, refreshMaxAge);
        response.addHeader("Set-Cookie", refresh.toString());

    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie(CookieProperties.ACCESS_TOKEN_NAME, "", 0).toString());
        response.addHeader("Set-Cookie", buildCookie(CookieProperties.REFRESH_TOKEN_NAME, "", 0).toString());
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .domain(cookieProperties.domain())
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}
