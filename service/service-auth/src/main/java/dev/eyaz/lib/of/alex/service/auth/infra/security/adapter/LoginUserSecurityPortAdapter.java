package dev.eyaz.lib.of.alex.service.auth.infra.security.adapter;

import dev.eyaz.lib.of.alex.service.auth.core.enums.UserRole;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.handler.LoginUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port.LoginUserSecurityPort;
import dev.eyaz.lib.of.alex.service.auth.infra.security.config.JwtProperties;
import dev.eyaz.lib.of.alex.service.auth.infra.security.token.JwtTokenService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class LoginUserSecurityPortAdapter implements LoginUserSecurityPort {

    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public LoginUserSecurityPortAdapter(JwtTokenService jwtTokenService, JwtProperties jwtProperties) {
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public LoginUser generateAccessAndRefreshTokens(LoginUser usecase) {
        Date now = new Date();
        Date accessExpiry = new Date(now.getTime() + jwtProperties.accessTokenExpirationMs());
        Date refreshExpiry = new Date(now.getTime() + jwtProperties.refreshTokenExpirationMs());

        String accessToken = jwtTokenService.generateAccessToken(
                usecase.getUserId(),
                usecase.getUsername(),
                usecase.getRole().stream().map(UserRole::name).toList(),
                now,
                accessExpiry);
        String refreshToken = jwtTokenService.generateRefreshToken(
                usecase.getUserId(),
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
