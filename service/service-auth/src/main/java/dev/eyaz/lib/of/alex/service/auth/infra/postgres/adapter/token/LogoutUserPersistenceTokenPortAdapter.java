package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.token;

import dev.eyaz.lib.of.alex.service.auth.core.exception.InvalidTokenException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.handler.LogoutUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.port.LogoutUserPersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.RefreshTokenEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LogoutUserPersistenceTokenPortAdapter implements LogoutUserPersistenceTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutUserPersistenceTokenPortAdapter(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public LogoutUser deleteRefreshTokenByToken(LogoutUser useCase) {
        Optional<RefreshTokenEntity> optional =
                refreshTokenRepository.findByToken(useCase.getRefreshToken());

        if (optional.isEmpty()) {
            return useCase;
        }

        RefreshTokenEntity entity = optional.get();

        if (!entity.getUserId().equals(useCase.getUserId())) {
            throw new InvalidTokenException("Token does not belong to the requesting user");
        }

        refreshTokenRepository.delete(entity);
        return useCase;
    }
}
