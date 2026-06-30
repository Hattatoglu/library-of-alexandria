package dev.eyaz.lib.of.alex.service.auth.infra.postgres.schedular;

import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class RefreshTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "${scheduler.refresh-token-cleanup-cron}")
    @Transactional
    public void cleanupExpiredTokens() {

        log.info("action=refresh_token_cleanup_started");
        try {
            int deletedCount = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

            log.info("action=refresh_token_cleanup_completed deletedCount={}", deletedCount);
        } catch (Exception ex) {
            log.error("action=refresh_token_cleanup_failed error={}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
