package dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response;

public record CreateUserResponse (
        String accessTokenExpiresAt,
        String refreshTokenExpiresAt
) {}
