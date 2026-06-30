package dev.eyaz.lib.of.alex.service.auth.infra.security.adapter;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCase;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCaseSecurityPort;
import dev.eyaz.lib.of.alex.service.auth.infra.security.config.JwtProperties;
import dev.eyaz.lib.of.alex.service.auth.infra.security.token.AccessTokenService;
import dev.eyaz.lib.of.alex.service.auth.infra.security.token.RefreshTokenService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class RefreshTokenUseCaseSecurityPortAdapter implements RefreshTokenUseCaseSecurityPort {

    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    public RefreshTokenUseCaseSecurityPortAdapter(AccessTokenService accessTokenService, RefreshTokenService refreshTokenService, JwtProperties jwtProperties) {
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public RefreshTokenUseCase generateAccessAndRefreshToken(RefreshTokenUseCase usecase) {
        Date now = new Date();
        Date accessExpiry = new Date(now.getTime() + jwtProperties.accessTokenExpirationMs());
        LocalDateTime refreshExpiry = LocalDateTime.now()
                .plusSeconds(jwtProperties.refreshTokenExpirationMs() / 1000);

        String accessToken = accessTokenService.generateAccessToken(
                usecase.getUserId(),
                usecase.getUsername(),
                usecase.getRoles().stream().map(Role::name).toList(),
                now,
                accessExpiry);
        String refreshToken = refreshTokenService.generateRefreshToken();
        usecase.setNewAccessToken(accessToken);
        usecase.setNewRefreshToken(refreshToken);
        usecase.setAccessTokenExpiresAt(LocalDateTime.ofInstant(
                accessExpiry.toInstant(),
                ZoneId.systemDefault()
        ));
        usecase.setRefreshTokenExpiresAt(refreshExpiry);

        return usecase;
    }
}
