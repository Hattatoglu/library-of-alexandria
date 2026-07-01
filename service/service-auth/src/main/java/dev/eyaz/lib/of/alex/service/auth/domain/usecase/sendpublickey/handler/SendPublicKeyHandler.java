package dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.port.SendPublicKeySecurityPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SendPublicKeyHandler implements UseCaseHandler<SendPublicKey> {

    private static final Logger log = LoggerFactory.getLogger(SendPublicKeyHandler.class);

    private final SendPublicKeySecurityPort sendPublicKeySecurityPort;

    public SendPublicKeyHandler(SendPublicKeySecurityPort sendPublicKeySecurityPort) {
        this.sendPublicKeySecurityPort = sendPublicKeySecurityPort;
    }

    @Override
    public SendPublicKey handle(SendPublicKey usecase) {
        log.debug("action=public_key_requested");
        //to avoid complexity Algorithm parameter set as hardcoded.
        usecase.setAlgorithm("RSA256");
        return sendPublicKeySecurityPort.getPublicKey(usecase);
    }
}
