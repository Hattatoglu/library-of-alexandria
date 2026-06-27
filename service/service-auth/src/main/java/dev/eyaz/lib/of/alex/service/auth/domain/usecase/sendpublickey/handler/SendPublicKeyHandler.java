package dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.port.SendPublicKeySecurityPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SendPublicKeyHandler implements UseCaseHandler<SendPublicKey> {

    private final SendPublicKeySecurityPort sendPublicKeySecurityPort;

    public SendPublicKeyHandler(SendPublicKeySecurityPort sendPublicKeySecurityPort) {
        this.sendPublicKeySecurityPort = sendPublicKeySecurityPort;
    }

    @Override
    public SendPublicKey handle(SendPublicKey usecase) {
        //to avoid complexity Algorithm parameter set as hardcoded.
        usecase.setAlgorithm("RSA256");
        return sendPublicKeySecurityPort.getPublicKey(usecase);
    }
}
