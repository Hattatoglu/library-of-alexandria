package dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.port.FindUserPersistenceAuthPort;
import org.springframework.stereotype.Service;

@Service
public class FindUserHandler implements UseCaseHandler<FindUser> {

    private final FindUserPersistenceAuthPort findUserPersistenceAuthPort;

    public FindUserHandler(FindUserPersistenceAuthPort findUserPersistenceAuthPort) {
        this.findUserPersistenceAuthPort = findUserPersistenceAuthPort;
    }

    @Override
    public FindUser handle(FindUser usecase) {
        return findUserPersistenceAuthPort.findUserByUsername(usecase);
    }
}
