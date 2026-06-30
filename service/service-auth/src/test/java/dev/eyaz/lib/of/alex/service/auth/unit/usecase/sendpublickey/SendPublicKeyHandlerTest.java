package dev.eyaz.lib.of.alex.service.auth.unit.usecase.sendpublickey;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.handler.SendPublicKey;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.handler.SendPublicKeyHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.port.SendPublicKeySecurityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SendPublicKeyHandler — Unit Tests")
class SendPublicKeyHandlerTest {

    private static class FakeSendPublicKeySecurityPort implements SendPublicKeySecurityPort {
        @Override
        public SendPublicKey getPublicKey(SendPublicKey usecase) {
            usecase.setPublicKey("-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG...stub...\n-----END PUBLIC KEY-----");
            return usecase;
        }
    }

    private SendPublicKeyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SendPublicKeyHandler(new FakeSendPublicKeySecurityPort());
    }

    @Test
    @DisplayName("Handler always sets the RSA256 algorithm")
    void shouldHardcodeAlgorithmAsRSA256() {
        SendPublicKey result = handler.handle(new SendPublicKey());

        assertThat(result.getAlgorithm()).isEqualTo("RSA256");
    }

    @Test
    @DisplayName("Public key is retrieved from the port and set")
    void shouldReturnPublicKeyFromPort() {
        SendPublicKey result = handler.handle(new SendPublicKey());

        assertThat(result.getPublicKey())
                .isNotBlank()
                .contains("BEGIN PUBLIC KEY");
    }

    @Test
    @DisplayName("Algorithm and publicKey are both populated in the response")
    void shouldReturnBothFieldsPopulated() {
        SendPublicKey result = handler.handle(new SendPublicKey());

        assertThat(result.getAlgorithm()).isNotBlank();
        assertThat(result.getPublicKey()).isNotBlank();
    }
}
