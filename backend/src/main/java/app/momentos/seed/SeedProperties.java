package app.momentos.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param enabled  liga o seed de demonstração (perfil dev)
 * @param password senha usada por todas as contas de exemplo
 */
@ConfigurationProperties(prefix = "momentos.seed")
public record SeedProperties(Boolean enabled, String password) {

    public SeedProperties {
        enabled = enabled != null && enabled;
        password = password == null || password.isBlank() ? "momentos123" : password;
    }
}
