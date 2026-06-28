package dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response;

public record LoginUserResponse (
        String accessTokenExpiresAt,
        String refreshTokenExpiresAt
) {}
