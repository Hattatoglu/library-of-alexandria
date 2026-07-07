package dev.eyaz.lib.of.alex.service.auth.infra.security.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CookieProperties.class})
public class AppConfig {}
