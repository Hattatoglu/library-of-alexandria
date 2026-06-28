package dev.eyaz.lib.of.alex.service.auth.infra.security.adapter;

import dev.eyaz.lib.of.alex.service.auth.core.enums.UserRole;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCase;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCaseSecurityPort;
import dev.eyaz.lib.of.alex.service.auth.infra.security.config.JwtProperties;
import dev.eyaz.lib.of.alex.service.auth.infra.security.token.JwtTokenService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class RefreshTokenUseCaseSecurityPortAdapter implements RefreshTokenUseCaseSecurityPort {

    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public RefreshTokenUseCaseSecurityPortAdapter(JwtTokenService jwtTokenService, JwtProperties jwtProperties) {
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public RefreshTokenUseCase generateAccessAndRefreshToken(RefreshTokenUseCase usecase) {
        Date now = new Date();
        Date accessExpiry = new Date(now.getTime() + jwtProperties.accessTokenExpirationMs());
        Date refreshExpiry = new Date(now.getTime() + jwtProperties.refreshTokenExpirationMs());

        String accessToken = jwtTokenService.generateAccessToken(
                usecase.getUserId(),
                usecase.getUsername(),
                usecase.getRoles().stream().map(UserRole::name).toList(),
                now,
                accessExpiry);
        String refreshToken = jwtTokenService.generateRefreshToken(
                usecase.getUserId(),
                now,
                refreshExpiry);
        usecase.setNewAccessToken(accessToken);
        usecase.setNewRefreshToken(refreshToken);
        usecase.setAccessTokenExpiresAt(LocalDateTime.ofInstant(
                accessExpiry.toInstant(),
                ZoneId.systemDefault()
        ));
        usecase.setRefreshTokenExpiresAt(LocalDateTime.ofInstant(
                refreshExpiry.toInstant(),
                ZoneId.systemDefault()
        ));

        return usecase;
    }
}
