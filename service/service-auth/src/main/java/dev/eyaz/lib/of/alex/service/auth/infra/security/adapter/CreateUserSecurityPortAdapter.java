package dev.eyaz.lib.of.alex.service.auth.infra.security.adapter;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.handler.CreateUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.port.CreateUserSecurityPort;
import dev.eyaz.lib.of.alex.service.auth.infra.security.config.JwtProperties;
import dev.eyaz.lib.of.alex.service.auth.infra.security.token.JwtTokenService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class CreateUserSecurityPortAdapter implements CreateUserSecurityPort {

    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public CreateUserSecurityPortAdapter(JwtTokenService jwtTokenService, JwtProperties jwtProperties) {
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public CreateUser generateAccessAndRefreshTokens(CreateUser usecase) {
        Date now = new Date();
        Date accessExpiry = new Date(now.getTime() + jwtProperties.accessTokenExpirationMs());
        Date refreshExpiry = new Date(now.getTime() + jwtProperties.refreshTokenExpirationMs());

        String accessToken = jwtTokenService.generateAccessToken(
                usecase.getUserId(),
                usecase.getUsername(),
                usecase.getRole().getValue(),
                now,
                accessExpiry);
        String refreshToken = jwtTokenService.generateAccessToken(
                usecase.getUserId(),
                usecase.getUsername(),
                usecase.getRole().getValue(),
                now,
                refreshExpiry);
        usecase.setAccessToken(accessToken);
        usecase.setRefreshToken(refreshToken);
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
