package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.token;

import dev.eyaz.lib.of.alex.service.auth.core.exception.InvalidTokenException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCase;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.RefreshTokenEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class RefreshTokenUseCasePersistenceTokenPortAdapter implements RefreshTokenUseCasePersistenceTokenPort {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenUseCasePersistenceTokenPortAdapter.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthMetrics authMetrics;

    public RefreshTokenUseCasePersistenceTokenPortAdapter(RefreshTokenRepository refreshTokenRepository, AuthMetrics authMetrics) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authMetrics = authMetrics;
    }

    @Override
    public RefreshTokenUseCase findRefreshTokenByToken(RefreshTokenUseCase useCase) {
        Optional<RefreshTokenEntity> optional = refreshTokenRepository.findByToken(useCase.getRefreshToken());
        if(optional.isPresent()) {
            RefreshTokenEntity entity = optional.get();
            if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
                refreshTokenRepository.delete(entity);
                authMetrics.incrementTokenRefreshFailureTokenExpired();
                log.warn("action=token_refresh_token_expired userId={}", entity.getUserId());
                throw new InvalidTokenException("Refresh token expired");
            }
            useCase.setUserId(entity.getUserId());
            log.debug("action=token_refresh_token_found userId={}", entity.getUserId());
        } else {
            authMetrics.incrementTokenRefreshFailureTokenNotFound();
            log.warn("action=token_refresh_token_not_found");
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
        log.debug("action=token_refresh_rotated userId={}", useCase.getUserId());
        return useCase;
    }
}
