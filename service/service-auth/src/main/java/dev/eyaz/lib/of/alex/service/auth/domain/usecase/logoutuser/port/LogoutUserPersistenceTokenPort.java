package dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.port;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.handler.LogoutUser;

public interface LogoutUserPersistenceTokenPort {
    LogoutUser deleteRefreshTokenByToken(LogoutUser useCase);
}
