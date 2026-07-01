package dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.core.exception.UserAlreadyExistsException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.port.SignUpUserPersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SignUpUserHandler implements UseCaseHandler<SignUpUser> {

    private static final Logger log = LoggerFactory.getLogger(SignUpUserHandler.class);

    private final SignUpUserPersistenceAuthPort signUpUserPersistenceAuthPort;
    private final AuthMetrics authMetrics;

    public SignUpUserHandler(SignUpUserPersistenceAuthPort signUpUserPersistenceAuthPort, AuthMetrics authMetrics) {
        this.signUpUserPersistenceAuthPort = signUpUserPersistenceAuthPort;
        this.authMetrics = authMetrics;
    }

    @Override
    public SignUpUser handle(SignUpUser usecase) {
        MDC.put("username", usecase.getUsername());
        Timer.Sample timer = authMetrics.startTimer();
        try {
            log.info("action=sign_up_attempt username={}", usecase.getUsername());

            SignUpUser checkedUser = checkUsernameAndMailExist(usecase);
            SignUpUser initializedUser = initiateUser(checkedUser);
            SignUpUser saved = saveUserDetails(initializedUser);

            authMetrics.incrementSignUpSuccess();
            log.info("action=sign_up_success username={} userId={}", saved.getUsername(), saved.getUserId());
            return saved;
        } catch (UserAlreadyExistsException ex) {
            log.warn("action=sign_up_rejected username={} reason={}", usecase.getUsername(), ex.getMessage());
            throw ex;
        } finally {
            authMetrics.stopSignUpTimer(timer);
            MDC.remove("username");
        }
    }

    private SignUpUser checkUsernameAndMailExist(SignUpUser usecase) {
        return signUpUserPersistenceAuthPort.checkUsernameAndMail(usecase);
    }

    private SignUpUser initiateUser(SignUpUser usecase) {
        usecase.setUserId(UUID.randomUUID());
        usecase.setRole(Set.of(Role.ROLE_CUSTOM_USER));
        usecase.setAccountNonExpired(true);
        usecase.setAccountNonLocked(true);
        usecase.setCredentialsNonExpired(true);
        usecase.setEnabled(true);
        return usecase;
    }

    private SignUpUser saveUserDetails(SignUpUser usecase) {
        return signUpUserPersistenceAuthPort.saveUser(usecase);
    }
}
