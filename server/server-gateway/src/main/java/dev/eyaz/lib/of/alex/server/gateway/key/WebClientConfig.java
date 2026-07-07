package dev.eyaz.lib.of.alex.server.gateway.key;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient serviceAuthWebClient(GatewayAuthProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.serviceAuthBaseUrl())
                .build();
    }
}
