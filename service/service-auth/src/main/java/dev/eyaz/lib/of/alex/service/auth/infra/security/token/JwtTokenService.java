package dev.eyaz.lib.of.alex.service.auth.infra.security.token;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public interface JwtTokenService {
    String generateAccessToken(UUID userId, String username, String role, Date now, Date expiry);
    String generateRefreshToken(UUID userId);
    UUID extractUserIdFromRefreshToken(String token);
    boolean validateRefreshToken(String token);
    String getPublicKeyPem();
    LocalDateTime getRefreshTokenExpiry();
}
