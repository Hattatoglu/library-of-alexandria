package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.token;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.handler.LoginUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port.LoginUserRefreshTokenPersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.RefreshTokenEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoginUserRefreshTokenPersistenceTokenPortAdapter implements LoginUserRefreshTokenPersistenceTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;

    private static final Logger log = LoggerFactory.getLogger(LoginUserRefreshTokenPersistenceTokenPortAdapter.class);

    public LoginUserRefreshTokenPersistenceTokenPortAdapter(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public LoginUser persistRefreshToken(LoginUser usecase) {

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(usecase.getUserId());
        entity.setToken(usecase.getRefreshToken());
        entity.setExpiresAt(usecase.getRefreshTokenExpiresAt());

        refreshTokenRepository.save(entity);
        log.debug("action=login_refresh_token_persisted userId={}", usecase.getUserId());

        return usecase;
    }
}
