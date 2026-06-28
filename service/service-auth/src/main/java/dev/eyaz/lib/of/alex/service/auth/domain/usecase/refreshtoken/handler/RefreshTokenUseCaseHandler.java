package dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCaseSecurityPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenUseCaseHandler implements UseCaseHandler<RefreshTokenUseCase> {

    private final RefreshTokenUseCasePersistenceTokenPort refreshTokenUseCasePersistenceTokenPort;
    private final RefreshTokenUseCasePersistenceAuthPort refreshTokenUseCasePersistenceAuthPort;
    private final RefreshTokenUseCaseSecurityPort refreshTokenUseCaseSecurityPort;

    public RefreshTokenUseCaseHandler(RefreshTokenUseCasePersistenceTokenPort refreshTokenUseCasePersistenceTokenPort, RefreshTokenUseCasePersistenceAuthPort refreshTokenUseCasePersistenceAuthPort,
                                      RefreshTokenUseCaseSecurityPort refreshTokenUseCaseSecurityPort) {
        this.refreshTokenUseCasePersistenceTokenPort = refreshTokenUseCasePersistenceTokenPort;
        this.refreshTokenUseCasePersistenceAuthPort = refreshTokenUseCasePersistenceAuthPort;
        this.refreshTokenUseCaseSecurityPort = refreshTokenUseCaseSecurityPort;
    }

    @Override
    public RefreshTokenUseCase handle(RefreshTokenUseCase usecase) {
        RefreshTokenUseCase checkToken = findRefreshToken(usecase);
        RefreshTokenUseCase userDetails = findUserDetails(checkToken);
        RefreshTokenUseCase generateNewTokens = generateTokens(userDetails);
        return persistAndDeleteToken(generateNewTokens);
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
