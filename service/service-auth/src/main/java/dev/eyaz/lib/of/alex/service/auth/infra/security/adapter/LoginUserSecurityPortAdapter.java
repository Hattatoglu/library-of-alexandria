package dev.eyaz.lib.of.alex.service.auth.infra.security.adapter;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.handler.LoginUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port.LoginUserSecurityPort;
import dev.eyaz.lib.of.alex.service.auth.infra.security.config.JwtProperties;
import dev.eyaz.lib.of.alex.service.auth.infra.security.token.AccessTokenService;
import dev.eyaz.lib.of.alex.service.auth.infra.security.token.RefreshTokenService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class LoginUserSecurityPortAdapter implements LoginUserSecurityPort {

    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    public LoginUserSecurityPortAdapter(AccessTokenService accessTokenService, RefreshTokenService refreshTokenService, JwtProperties jwtProperties) {
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public LoginUser generateAccessAndRefreshTokens(LoginUser usecase) {
        Date now = new Date();
        Date accessExpiry = new Date(now.getTime() + jwtProperties.accessTokenExpirationMs());
        Date refreshExpiry = new Date(now.getTime() + jwtProperties.refreshTokenExpirationMs());

        String accessToken = accessTokenService.generateAccessToken(
                usecase.getUserId(),
                usecase.getUsername(),
                usecase.getRole().stream().map(Role::name).toList(),
                now,
                accessExpiry);
        String refreshToken = refreshTokenService.generateRefreshToken();
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
