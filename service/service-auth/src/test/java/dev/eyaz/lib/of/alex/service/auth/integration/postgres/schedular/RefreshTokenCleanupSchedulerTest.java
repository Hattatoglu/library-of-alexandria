package dev.eyaz.lib.of.alex.service.auth.integration.postgres.schedular;

import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.schedular.RefreshTokenCleanupScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RefreshTokenCleanupScheduler — Unit Tests")
class RefreshTokenCleanupSchedulerTest {

    private RefreshTokenRepository repository;
    private RefreshTokenCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        repository  = mock(RefreshTokenRepository.class);
        scheduler   = new RefreshTokenCleanupScheduler(repository);
    }

    @Test
    @DisplayName("Repository delete method is called when expired tokens exist")
    void shouldDeleteExpiredTokens() {
        when(repository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5);

        scheduler.cleanupExpiredTokens();

        verify(repository, times(1)).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Completes without error when there are no tokens to delete")
    void shouldCompleteSuccessfullyWhenNoExpiredTokens() {
        when(repository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(0);

        assertThatCode(() -> scheduler.cleanupExpiredTokens()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Scheduler propagates the exception when the repository throws (job failure must be visible)")
    void shouldPropagateExceptionFromRepository() {
        when(repository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> scheduler.cleanupExpiredTokens())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB connection lost");
    }

}
