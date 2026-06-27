package dev.eyaz.lib.of.alex.service.auth.infra.security.adapter;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.handler.SendPublicKey;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.port.SendPublicKeySecurityPort;
import dev.eyaz.lib.of.alex.service.auth.infra.security.token.JwtTokenService;
import org.springframework.stereotype.Component;

@Component
public class SendPublicKeySecurityPortAdapter implements SendPublicKeySecurityPort {

    private final JwtTokenService jwtTokenService;

    public SendPublicKeySecurityPortAdapter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public SendPublicKey getPublicKey(SendPublicKey usecase) {
        String publicKey = jwtTokenService.getPublicKeyPem();
        usecase.setPublicKey(publicKey);
        return usecase;
    }
}
