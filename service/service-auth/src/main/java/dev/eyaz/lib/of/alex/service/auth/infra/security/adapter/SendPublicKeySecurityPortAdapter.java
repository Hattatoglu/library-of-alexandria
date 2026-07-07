package dev.eyaz.lib.of.alex.service.auth.infra.security.adapter;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.handler.SendPublicKey;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.port.SendPublicKeySecurityPort;
import dev.eyaz.lib.of.alex.service.auth.infra.security.token.AccessTokenService;
import org.springframework.stereotype.Component;

@Component
public class SendPublicKeySecurityPortAdapter implements SendPublicKeySecurityPort {

    private final AccessTokenService accessTokenService;

    public SendPublicKeySecurityPortAdapter(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    @Override
    public SendPublicKey getPublicKey(SendPublicKey usecase) {
        String publicKey = accessTokenService.getPublicKeyPem();
        usecase.setPublicKey(publicKey);
        return usecase;
    }
}
