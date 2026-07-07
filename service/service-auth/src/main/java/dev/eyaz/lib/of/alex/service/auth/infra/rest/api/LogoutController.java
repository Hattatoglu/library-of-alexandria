package dev.eyaz.lib.of.alex.service.auth.infra.rest.api;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.handler.LogoutUser;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.cookie.CookieProvider;
import dev.eyaz.lib.of.alex.service.auth.infra.security.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class LogoutController {

    private final UseCaseHandler<LogoutUser> useCaseHandler;
    private final CookieProvider cookieProvider;

    public LogoutController(UseCaseHandler<LogoutUser> useCaseHandler, CookieProvider cookieProvider) {
        this.useCaseHandler = useCaseHandler;
        this.cookieProvider = cookieProvider;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                        HttpServletResponse response) {

        String refreshToken = extractCookieValue(request);
        String userId = request.getHeader("X-User-Id");

        LogoutUser usecase = new LogoutUser();
        usecase.setUserId(UUID.fromString(userId));
        usecase.setRefreshToken(refreshToken);

        LogoutUser answer = useCaseHandler.handle(usecase);

        cookieProvider.clearAuthCookies(response);

        return ResponseEntity.noContent().build();
    }

    private String extractCookieValue(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new IllegalArgumentException("No cookies present in request");
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(CookieProperties.REFRESH_TOKEN_NAME))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cookie not found: "
                        + CookieProperties.REFRESH_TOKEN_NAME));
    }

}
