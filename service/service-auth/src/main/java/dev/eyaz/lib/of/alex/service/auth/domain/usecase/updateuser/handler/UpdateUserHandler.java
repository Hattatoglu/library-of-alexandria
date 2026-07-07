package dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.port.UpdateUserPersistAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class UpdateUserHandler implements UseCaseHandler<UpdateUser> {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserHandler.class);

    private final UpdateUserPersistAuthPort updateUserPersistAuthPort;
    private final AuthMetrics authMetrics;

    public UpdateUserHandler(UpdateUserPersistAuthPort updateUserPersistAuthPort, AuthMetrics authMetrics) {
        this.updateUserPersistAuthPort = updateUserPersistAuthPort;
        this.authMetrics = authMetrics;
    }

    @Override
    public UpdateUser handle(UpdateUser usecase) {
        MDC.put("username", usecase.getUsername());
        MDC.put("userId", usecase.getUserId() != null ? usecase.getUserId().toString() : "unknown");
        try {
            log.info("action=role_update_attempt username={} newRole={}",
                    usecase.getUsername(), usecase.getNewRole());

            UpdateUser result = updateUserPersistAuthPort.updateUserRole(usecase);

            authMetrics.incrementRoleUpdateSuccess();
            log.info("action=role_update_success username={} updatedRoles={}",
                    result.getUsername(), result.getRole());
            return result;
        } finally {
            MDC.remove("username");
            MDC.remove("userId");
        }
    }
}
