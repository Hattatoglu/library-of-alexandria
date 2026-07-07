package dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCase;

public interface RefreshTokenUseCaseSecurityPort {

    RefreshTokenUseCase generateAccessAndRefreshToken(RefreshTokenUseCase usecase);
}
