package dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCaseSecurityPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenUseCaseHandler implements UseCaseHandler<RefreshTokenUseCase> {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenUseCaseHandler.class);

    private final RefreshTokenUseCasePersistenceTokenPort refreshTokenUseCasePersistenceTokenPort;
    private final RefreshTokenUseCasePersistenceAuthPort refreshTokenUseCasePersistenceAuthPort;
    private final RefreshTokenUseCaseSecurityPort refreshTokenUseCaseSecurityPort;
    private final AuthMetrics authMetrics;

    public RefreshTokenUseCaseHandler(RefreshTokenUseCasePersistenceTokenPort refreshTokenUseCasePersistenceTokenPort, RefreshTokenUseCasePersistenceAuthPort refreshTokenUseCasePersistenceAuthPort,
                                      RefreshTokenUseCaseSecurityPort refreshTokenUseCaseSecurityPort, AuthMetrics authMetrics) {
        this.refreshTokenUseCasePersistenceTokenPort = refreshTokenUseCasePersistenceTokenPort;
        this.refreshTokenUseCasePersistenceAuthPort = refreshTokenUseCasePersistenceAuthPort;
        this.refreshTokenUseCaseSecurityPort = refreshTokenUseCaseSecurityPort;
        this.authMetrics = authMetrics;
    }

    @Override
    public RefreshTokenUseCase handle(RefreshTokenUseCase usecase) {
        Timer.Sample timer = authMetrics.startTimer();
        try {
            log.info("action=token_refresh_attempt");

            RefreshTokenUseCase checkToken = findRefreshToken(usecase);
            MDC.put("userId", checkToken.getUserId().toString());

            RefreshTokenUseCase userDetails = findUserDetails(checkToken);
            MDC.put("username", userDetails.getUsername());

            RefreshTokenUseCase generateNewTokens = generateTokens(userDetails);
            RefreshTokenUseCase result = persistAndDeleteToken(generateNewTokens);

            authMetrics.incrementTokenRefreshSuccess();
            log.info("action=token_refresh_success userId={} username={} newAccessTokenExpiresAt={}",
                    result.getUserId(), result.getUsername(), result.getAccessTokenExpiresAt());
            return result;
        } finally {
            authMetrics.stopTokenRefreshTimer(timer);
            MDC.remove("userId");
            MDC.remove("username");
        }
    }

    private RefreshTokenUseCase findRefreshToken(RefreshTokenUseCase usecase) {
        return refreshTokenUseCasePersistenceTokenPort.findRefreshTokenByToken(usecase);
    }

    private RefreshTokenUseCase findUserDetails(RefreshTokenUseCase useCase) {
        return refreshTokenUseCasePersistenceAuthPort.findUserDetails(useCase);
    }

    private RefreshTokenUseCase generateTokens(RefreshTokenUseCase usecase) {
        return refreshTokenUseCaseSecurityPort.generateAccessAndRefreshToken(usecase);
    }

    private RefreshTokenUseCase persistAndDeleteToken(RefreshTokenUseCase usecase) {
        return refreshTokenUseCasePersistenceTokenPort
                .saveNewRefreshTokenAndDeleteOldRefreshToken(usecase);
    }
}
