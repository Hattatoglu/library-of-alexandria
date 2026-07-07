package dev.eyaz.lib.of.alex.service.auth.infra.rest.api;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCase;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.cookie.CookieProvider;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response.LoginUserResponse;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response.RefreshTokenResponse;
import dev.eyaz.lib.of.alex.service.auth.infra.security.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
public class RefreshAccessTokenController {

    private final RefreshTokenUseCaseHandler useCaseHandler;
    private final CookieProvider cookieProvider;

    public RefreshAccessTokenController(RefreshTokenUseCaseHandler useCaseHandler,
                                        CookieProvider cookieProvider) {
        this.useCaseHandler = useCaseHandler;
        this.cookieProvider = cookieProvider;
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshAccessToken(HttpServletRequest request,
                                                                   HttpServletResponse response) {

        String refreshToken = extractCookieValue(request);

        RefreshTokenUseCase useCase = new RefreshTokenUseCase();
        useCase.setRefreshToken(refreshToken);

        RefreshTokenUseCase answer = useCaseHandler.handle(useCase);

        cookieProvider.httpOnlyRefreshAndAccessTokenProvider(
                response,
                answer.getNewAccessToken(),
                answer.getNewRefreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .body(new RefreshTokenResponse(
                        answer.getAccessTokenExpiresAt().toString(),
                        answer.getRefreshTokenExpiresAt().toString()
                ));
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
