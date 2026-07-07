package dev.eyaz.lib.of.alex.service.auth.infra.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Central metric definitions for service-auth.
 * <p>
 * Built on Micrometer, exposed through Spring Boot Actuator at {@code /actuator/prometheus}
 * (see {@code application.yaml -> management.*}). Handlers, adapters, and the scheduled
 * cleanup job inject this component instead of registering meters ad hoc, so every metric
 * name, tag, and description lives in one place.
 * <p>
 * All counters are scoped to a single use case outcome (success or a specific failure
 * reason) rather than a single generic "errors" counter with a label, so that dashboards
 * and alerts can be built directly on a named series without needing to know which tag
 * values exist.
 */
@Component
public class AuthMetrics {

    // ── Counters: Sign-up ────────────────────────────────────────────────────
    private final Counter signUpSuccess;
    private final Counter signUpFailureDuplicateUsername;
    private final Counter signUpFailureDuplicateEmail;

    // ── Counters: Login ──────────────────────────────────────────────────────
    private final Counter loginSuccess;
    private final Counter loginFailureInvalidCredentials;

    // ── Counters: Logout ─────────────────────────────────────────────────────
    private final Counter logoutSuccess;
    private final Counter logoutFailureTokenMismatch;
    private final Counter logoutFailureTokenNotFound;

    // ── Counters: Token Refresh ──────────────────────────────────────────────
    private final Counter tokenRefreshSuccess;
    private final Counter tokenRefreshFailureTokenNotFound;
    private final Counter tokenRefreshFailureTokenExpired;
    private final Counter tokenRefreshFailureUserNotFound;

    // ── Counters: Role Update ────────────────────────────────────────────────
    private final Counter roleUpdateSuccess;
    private final Counter roleUpdateFailureUserNotFound;

    // ── Counters: Find User ──────────────────────────────────────────────────
    private final Counter findUserSuccess;
    private final Counter findUserFailureNotFound;

    // ── Counters: Refresh Token Cleanup Job ──────────────────────────────────
    private final Counter refreshTokenCleanupRuns;
    private final Counter refreshTokenCleanupFailures;
    private final DistributionSummary refreshTokenCleanupDeletedCount;

    // ── Timers ───────────────────────────────────────────────────────────────
    private final Timer signUpDuration;
    private final Timer loginDuration;
    private final Timer tokenRefreshDuration;
    private final Timer refreshTokenCleanupDuration;

    public AuthMetrics(MeterRegistry registry) {

        // Sign-up
        this.signUpSuccess = Counter.builder("auth.signup.success")
                .description("Number of successful user sign-ups")
                .register(registry);
        this.signUpFailureDuplicateUsername = Counter.builder("auth.signup.failure")
                .description("Number of failed sign-up attempts")
                .tag("reason", "duplicate_username")
                .register(registry);
        this.signUpFailureDuplicateEmail = Counter.builder("auth.signup.failure")
                .description("Number of failed sign-up attempts")
                .tag("reason", "duplicate_email")
                .register(registry);

        // Login
        this.loginSuccess = Counter.builder("auth.login.success")
                .description("Number of successful logins")
                .register(registry);
        this.loginFailureInvalidCredentials = Counter.builder("auth.login.failure")
                .description("Number of failed login attempts")
                .tag("reason", "invalid_credentials")
                .register(registry);

        // Logout
        this.logoutSuccess = Counter.builder("auth.logout.success")
                .description("Number of successful logouts")
                .register(registry);
        this.logoutFailureTokenMismatch = Counter.builder("auth.logout.failure")
                .description("Number of failed logout attempts")
                .tag("reason", "token_owner_mismatch")
                .register(registry);
        this.logoutFailureTokenNotFound = Counter.builder("auth.logout.failure")
                .description("Number of failed logout attempts")
                .tag("reason", "token_not_found")
                .register(registry);

        // Token Refresh
        this.tokenRefreshSuccess = Counter.builder("auth.token_refresh.success")
                .description("Number of successful refresh token rotations")
                .register(registry);
        this.tokenRefreshFailureTokenNotFound = Counter.builder("auth.token_refresh.failure")
                .description("Number of failed token refresh attempts")
                .tag("reason", "token_not_found")
                .register(registry);
        this.tokenRefreshFailureTokenExpired = Counter.builder("auth.token_refresh.failure")
                .description("Number of failed token refresh attempts")
                .tag("reason", "token_expired")
                .register(registry);
        this.tokenRefreshFailureUserNotFound = Counter.builder("auth.token_refresh.failure")
                .description("Number of failed token refresh attempts")
                .tag("reason", "user_not_found")
                .register(registry);

        // Role Update
        this.roleUpdateSuccess = Counter.builder("auth.role_update.success")
                .description("Number of successful role updates")
                .register(registry);
        this.roleUpdateFailureUserNotFound = Counter.builder("auth.role_update.failure")
                .description("Number of failed role update attempts")
                .tag("reason", "user_not_found")
                .register(registry);

        // Find User
        this.findUserSuccess = Counter.builder("auth.find_user.success")
                .description("Number of successful user lookups")
                .register(registry);
        this.findUserFailureNotFound = Counter.builder("auth.find_user.failure")
                .description("Number of failed user lookups")
                .tag("reason", "user_not_found")
                .register(registry);

        // Refresh Token Cleanup Job
        this.refreshTokenCleanupRuns = Counter.builder("auth.refresh_token_cleanup.runs")
                .description("Number of times the expired refresh token cleanup job has run")
                .register(registry);
        this.refreshTokenCleanupFailures = Counter.builder("auth.refresh_token_cleanup.failures")
                .description("Number of times the expired refresh token cleanup job has failed")
                .register(registry);
        this.refreshTokenCleanupDeletedCount = DistributionSummary
                .builder("auth.refresh_token_cleanup.deleted_count")
                .description("Number of expired refresh tokens deleted per cleanup run")
                .register(registry);

        // Timers
        this.signUpDuration = Timer.builder("auth.signup.duration")
                .description("Time taken to process a sign-up request, including password hashing")
                .publishPercentileHistogram()
                .register(registry);
        this.loginDuration = Timer.builder("auth.login.duration")
                .description("Time taken to process a login request, including token issuance")
                .publishPercentileHistogram()
                .register(registry);
        this.tokenRefreshDuration = Timer.builder("auth.token_refresh.duration")
                .description("Time taken to process a token refresh request")
                .publishPercentileHistogram()
                .register(registry);
        this.refreshTokenCleanupDuration = Timer.builder("auth.refresh_token_cleanup.duration")
                .description("Time taken to run the expired refresh token cleanup job")
                .publishPercentileHistogram()
                .register(registry);
    }

    // ── Sign-up ──────────────────────────────────────────────────────────────
    public void incrementSignUpSuccess()                  { signUpSuccess.increment(); }
    public void incrementSignUpFailureDuplicateUsername()  { signUpFailureDuplicateUsername.increment(); }
    public void incrementSignUpFailureDuplicateEmail()     { signUpFailureDuplicateEmail.increment(); }

    // ── Login ────────────────────────────────────────────────────────────────
    public void incrementLoginSuccess()                    { loginSuccess.increment(); }
    public void incrementLoginFailureInvalidCredentials()   { loginFailureInvalidCredentials.increment(); }

    // ── Logout ───────────────────────────────────────────────────────────────
    public void incrementLogoutSuccess()                   { logoutSuccess.increment(); }
    public void incrementLogoutFailureTokenMismatch()       { logoutFailureTokenMismatch.increment(); }
    public void incrementLogoutFailureTokenNotFound()       { logoutFailureTokenNotFound.increment(); }

    // ── Token Refresh ────────────────────────────────────────────────────────
    public void incrementTokenRefreshSuccess()              { tokenRefreshSuccess.increment(); }
    public void incrementTokenRefreshFailureTokenNotFound() { tokenRefreshFailureTokenNotFound.increment(); }
    public void incrementTokenRefreshFailureTokenExpired()  { tokenRefreshFailureTokenExpired.increment(); }
    public void incrementTokenRefreshFailureUserNotFound()  { tokenRefreshFailureUserNotFound.increment(); }

    // ── Role Update ──────────────────────────────────────────────────────────
    public void incrementRoleUpdateSuccess()                { roleUpdateSuccess.increment(); }
    public void incrementRoleUpdateFailureUserNotFound()     { roleUpdateFailureUserNotFound.increment(); }

    // ── Find User ────────────────────────────────────────────────────────────
    public void incrementFindUserSuccess()                  { findUserSuccess.increment(); }
    public void incrementFindUserFailureNotFound()           { findUserFailureNotFound.increment(); }

    // ── Refresh Token Cleanup Job ────────────────────────────────────────────
    public void incrementRefreshTokenCleanupRuns()           { refreshTokenCleanupRuns.increment(); }
    public void incrementRefreshTokenCleanupFailures()       { refreshTokenCleanupFailures.increment(); }
    public void recordRefreshTokenCleanupDeletedCount(int count) {
        refreshTokenCleanupDeletedCount.record(count);
    }

    // ── Timers ───────────────────────────────────────────────────────────────
    public Timer.Sample startTimer()                         { return Timer.start(); }
    public void stopSignUpTimer(Timer.Sample sample)         { sample.stop(signUpDuration); }
    public void stopLoginTimer(Timer.Sample sample)          { sample.stop(loginDuration); }
    public void stopTokenRefreshTimer(Timer.Sample sample)   { sample.stop(tokenRefreshDuration); }
    public void stopRefreshTokenCleanupTimer(Timer.Sample sample) { sample.stop(refreshTokenCleanupDuration); }
}
