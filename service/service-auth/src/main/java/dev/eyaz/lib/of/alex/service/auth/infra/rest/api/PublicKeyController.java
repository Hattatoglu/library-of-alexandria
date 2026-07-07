package dev.eyaz.lib.of.alex.service.auth.infra.rest.api;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.sendpublickey.handler.SendPublicKey;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response.PublicKeyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class PublicKeyController {

    private final UseCaseHandler<SendPublicKey> useCaseHandler;

    public PublicKeyController(UseCaseHandler<SendPublicKey> useCaseHandler) {
        this.useCaseHandler = useCaseHandler;
    }

    @GetMapping("/public-key")
    public ResponseEntity<PublicKeyResponse> getPublicKey() {
        SendPublicKey usecase = new SendPublicKey();

        SendPublicKey answer = useCaseHandler.handle(usecase);

        PublicKeyResponse response = new PublicKeyResponse();
        response.setPublicKey(answer.getPublicKey());
        response.setAlgorithm(answer.getAlgorithm());

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }
}
