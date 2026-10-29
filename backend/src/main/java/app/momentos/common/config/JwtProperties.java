package app.momentos.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração dos tokens.
 *
 * @param secret    chave HMAC em Base64; vazia em desenvolvimento, quando uma chave
 *                  aleatória é gerada na subida (os tokens não sobrevivem a um restart)
 * @param accessTtl validade do access token
 * @param refreshTtl validade do refresh token
 */
@ConfigurationProperties(prefix = "momentos.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {

    public JwtProperties {
        accessTtl = accessTtl == null ? Duration.ofMinutes(15) : accessTtl;
        refreshTtl = refreshTtl == null ? Duration.ofDays(7) : refreshTtl;
    }
}
