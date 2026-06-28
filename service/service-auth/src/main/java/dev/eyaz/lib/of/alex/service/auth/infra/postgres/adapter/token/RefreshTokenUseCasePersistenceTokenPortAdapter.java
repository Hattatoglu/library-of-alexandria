package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.token;

import dev.eyaz.lib.of.alex.service.auth.core.exception.InvalidTokenException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCase;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.RefreshTokenEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RefreshTokenUseCasePersistenceTokenPortAdapter implements RefreshTokenUseCasePersistenceTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenUseCasePersistenceTokenPortAdapter(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public RefreshTokenUseCase findRefreshTokenByToken(RefreshTokenUseCase useCase) {
        Optional<RefreshTokenEntity> optional = refreshTokenRepository.findByToken(useCase.getRefreshToken());
        if(optional.isPresent()) {
            useCase.setUserId(optional.get().getUserId());
        } else {
            throw new InvalidTokenException("Token Not Found : "+ useCase.getRefreshToken());
        }
        return useCase;
    }

    @Override
    public RefreshTokenUseCase saveNewRefreshTokenAndDeleteOldRefreshToken(RefreshTokenUseCase useCase) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setToken(useCase.getNewRefreshToken());
        entity.setExpiresAt(useCase.getRefreshTokenExpiresAt());
        entity.setUserId(useCase.getUserId());

        refreshTokenRepository.save(entity);

        refreshTokenRepository.deleteByToken(useCase.getRefreshToken());
        return useCase;
    }
}
