package dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.port;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.handler.SendPublicKey;

public interface SendPublicKeySecurityPort {
    SendPublicKey getPublicKey(SendPublicKey usecase);
}
