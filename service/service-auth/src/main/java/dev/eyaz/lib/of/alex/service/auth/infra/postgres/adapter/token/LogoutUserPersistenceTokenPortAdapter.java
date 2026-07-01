package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.token;

import dev.eyaz.lib.of.alex.service.auth.core.exception.InvalidTokenException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.handler.LogoutUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.port.LogoutUserPersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.RefreshTokenEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LogoutUserPersistenceTokenPortAdapter implements LogoutUserPersistenceTokenPort {

    private static final Logger log = LoggerFactory.getLogger(LogoutUserPersistenceTokenPortAdapter.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthMetrics authMetrics;

    public LogoutUserPersistenceTokenPortAdapter(RefreshTokenRepository refreshTokenRepository, AuthMetrics authMetrics) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authMetrics = authMetrics;
    }

    @Override
    public LogoutUser deleteRefreshTokenByToken(LogoutUser useCase) {
        Optional<RefreshTokenEntity> optional =
                refreshTokenRepository.findByToken(useCase.getRefreshToken());

        if (optional.isEmpty()) {
            authMetrics.incrementLogoutFailureTokenNotFound();
            log.warn("action=logout_token_not_found userId={}", useCase.getUserId());
            return useCase;
        }

        RefreshTokenEntity entity = optional.get();

        if (!entity.getUserId().equals(useCase.getUserId())) {
            authMetrics.incrementLogoutFailureTokenMismatch();
            log.warn("action=logout_token_owner_mismatch requestedUserId={} tokenOwnerId={}",
                    useCase.getUserId(), entity.getUserId());
            throw new InvalidTokenException("Token does not belong to the requesting user");
        }

        refreshTokenRepository.delete(entity);
        return useCase;
    }
}
