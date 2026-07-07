package dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port.LoginUserRefreshTokenPersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port.LoginUserSecurityPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoginUserHandler implements UseCaseHandler<LoginUser> {

    private static final Logger log = LoggerFactory.getLogger(LoginUserHandler.class);

    private final LoginUserSecurityPort loginUserSecurityPort;
    private final LoginUserRefreshTokenPersistenceTokenPort loginUserRefreshTokenPersistenceTokenPort;
    private final AuthMetrics authMetrics;

    public LoginUserHandler(LoginUserSecurityPort loginUserSecurityPort, LoginUserRefreshTokenPersistenceTokenPort loginUserRefreshTokenPersistenceTokenPort, AuthMetrics authMetrics) {
        this.loginUserSecurityPort = loginUserSecurityPort;
        this.loginUserRefreshTokenPersistenceTokenPort = loginUserRefreshTokenPersistenceTokenPort;
        this.authMetrics = authMetrics;
    }

    @Override
    public LoginUser handle(LoginUser usecase) {
        MDC.put("username", usecase.getUsername());
        MDC.put("userId", usecase.getUserId() != null ? usecase.getUserId().toString() : "unknown");
        Timer.Sample timer = authMetrics.startTimer();
        try {
            log.info("action=login_attempt username={}", usecase.getUsername());

            LoginUser generateToken = generateAccessAndRefreshToken(usecase);
            LoginUser persisted = persistRefreshToken(generateToken);

            authMetrics.incrementLoginSuccess();
            log.info("action=login_success username={} userId={} accessTokenExpiresAt={}",
                    persisted.getUsername(), persisted.getUserId(), persisted.getAccessTokenExpiresAt());
            return persisted;
        } finally {
            authMetrics.stopLoginTimer(timer);
            MDC.remove("username");
            MDC.remove("userId");
        }
    }

    private LoginUser generateAccessAndRefreshToken(LoginUser usecase) {
        return loginUserSecurityPort.generateAccessAndRefreshTokens(usecase);
    }

    private LoginUser persistRefreshToken(LoginUser usecase) {
        return loginUserRefreshTokenPersistenceTokenPort.persistRefreshToken(usecase);
    }
}
