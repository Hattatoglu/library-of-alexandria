package dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response;

public record RefreshTokenResponse (
        String accessTokenExpiresAt,
        String refreshTokenExpiresAt
) {}
