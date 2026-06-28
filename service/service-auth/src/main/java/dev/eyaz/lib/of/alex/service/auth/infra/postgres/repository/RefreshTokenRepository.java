package dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository;

import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    Optional<RefreshTokenEntity> findByToken(String token);
    void deleteByToken(String token);
}
