package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.handler.LoginUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port.LoginUserRefreshTokenPersistencePort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.RefreshTokenEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import org.springframework.stereotype.Component;

@Component
public class LoginUserRefreshTokenPersistencePortAdapter implements LoginUserRefreshTokenPersistencePort {

    private final RefreshTokenRepository refreshTokenRepository;

    public LoginUserRefreshTokenPersistencePortAdapter(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public LoginUser persistRefreshToken(LoginUser usecase) {

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(usecase.getUserId());
        entity.setToken(usecase.getRefreshToken());
        entity.setExpiresAt(usecase.getRefreshTokenExpiresAt());

        refreshTokenRepository.save(entity);

        return usecase;
    }
}
