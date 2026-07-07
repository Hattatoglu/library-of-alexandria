package dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository;

import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthRepository extends JpaRepository<UserAuthEntity, UUID> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<UserAuthEntity> findByUsername(String username);
    Optional<UserAuthEntity> findByUserId(UUID userId);
}
