package dev.eyaz.lib.of.alex.service.auth.infra.postgres.schedular;

import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import io.micrometer.core.instrument.Timer;
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
    private final AuthMetrics authMetrics;

    public RefreshTokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository, AuthMetrics authMetrics) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authMetrics = authMetrics;
    }

    @Scheduled(cron = "${scheduler.refresh-token-cleanup-cron}")
    @Transactional
    public void cleanupExpiredTokens() {
        Timer.Sample timer = authMetrics.startTimer();
        authMetrics.incrementRefreshTokenCleanupRuns();

        log.info("action=refresh_token_cleanup_started");
        try {
            int deletedCount = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

            authMetrics.recordRefreshTokenCleanupDeletedCount(deletedCount);
            log.info("action=refresh_token_cleanup_completed deletedCount={}", deletedCount);
        } catch (Exception ex) {
            authMetrics.incrementRefreshTokenCleanupFailures();
            log.error("action=refresh_token_cleanup_failed error={}", ex.getMessage(), ex);
            throw ex;
        } finally {
            authMetrics.stopRefreshTokenCleanupTimer(timer);
        }
    }
}
