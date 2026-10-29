package app.momentos.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Em desenvolvimento o Angular usa proxy (`/api` → 8080) e o CORS fica inativo;
 * a lista existe para quando o front for servido de outra origem.
 */
@ConfigurationProperties(prefix = "momentos.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
    }
}
