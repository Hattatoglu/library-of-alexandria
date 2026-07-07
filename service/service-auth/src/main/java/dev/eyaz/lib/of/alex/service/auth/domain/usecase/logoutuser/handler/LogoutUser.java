package dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCase;

import java.util.UUID;

public class LogoutUser implements UseCase {

    private UUID userId;
    private String refreshToken;

    public LogoutUser() {
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
