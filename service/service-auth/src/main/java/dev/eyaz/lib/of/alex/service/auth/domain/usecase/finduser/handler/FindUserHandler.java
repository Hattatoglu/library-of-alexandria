package dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.port.FindUserPersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FindUserHandler implements UseCaseHandler<FindUser> {

    private static final Logger log = LoggerFactory.getLogger(FindUserHandler.class);

    private final FindUserPersistenceAuthPort findUserPersistenceAuthPort;
    private final AuthMetrics authMetrics;

    public FindUserHandler(FindUserPersistenceAuthPort findUserPersistenceAuthPort, AuthMetrics authMetrics) {
        this.findUserPersistenceAuthPort = findUserPersistenceAuthPort;
        this.authMetrics = authMetrics;
    }

    @Override
    public FindUser handle(FindUser usecase) {
        log.debug("action=find_user_attempt username={}", usecase.getUsername());
        FindUser result = findUserPersistenceAuthPort.findUserByUsername(usecase);
        authMetrics.incrementFindUserSuccess();
        log.debug("action=find_user_success username={} userId={}", result.getUsername(), result.getUserId());
        return result;
    }
}
