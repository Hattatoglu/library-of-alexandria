package dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response;

public class PublicKeyResponse {

    private String publicKey;
    private String algorithm;

    public PublicKeyResponse() {
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
