package dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCase;

public class SendPublicKey implements UseCase {

    private String publicKey;
    private String algorithm;

    public SendPublicKey() {
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
