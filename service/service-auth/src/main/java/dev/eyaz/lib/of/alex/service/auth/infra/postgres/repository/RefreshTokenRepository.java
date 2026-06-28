package dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository;

import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
}
