package dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.port.LogoutUserPersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LogoutUserHandler implements UseCaseHandler<LogoutUser> {

    private static final Logger log = LoggerFactory.getLogger(LogoutUserHandler.class);

    private final LogoutUserPersistenceTokenPort logoutUserPersistenceTokenPort;
    private final AuthMetrics authMetrics;

    public LogoutUserHandler(LogoutUserPersistenceTokenPort logoutUserPersistenceTokenPort, AuthMetrics authMetrics) {
        this.logoutUserPersistenceTokenPort = logoutUserPersistenceTokenPort;
        this.authMetrics = authMetrics;
    }

    @Override
    public LogoutUser handle(LogoutUser usecase) {
        MDC.put("userId", usecase.getUserId() != null ? usecase.getUserId().toString() : "unknown");
        try {
            log.info("action=logout_attempt userId={}", usecase.getUserId());

            LogoutUser result = logoutUserPersistenceTokenPort.deleteRefreshTokenByToken(usecase);

            authMetrics.incrementLogoutSuccess();
            log.info("action=logout_success userId={}", usecase.getUserId());
            return result;
        } finally {
            MDC.remove("userId");
        }
    }
}
