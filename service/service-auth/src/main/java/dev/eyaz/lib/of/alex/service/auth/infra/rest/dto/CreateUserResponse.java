package dev.eyaz.lib.of.alex.service.auth.infra.rest.dto;

public record CreateUserResponse (
        String accessTokenExpiresAt,
        String refreshTokenExpiresAt
) {}
