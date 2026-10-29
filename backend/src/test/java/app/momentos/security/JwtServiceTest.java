package app.momentos.security;

import static org.assertj.core.api.Assertions.assertThat;

import app.momentos.common.config.JwtProperties;
import app.momentos.users.User;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService service = new JwtService(
            new JwtProperties(null, Duration.ofMinutes(15), Duration.ofDays(7)));

    @Test
    void token_emitido_carrega_identidade_do_usuario() {
        User user = new User("Felix", "felix@ufac.br", "hash");

        String token = service.issueAccessToken(user);
        var lido = service.readAccessToken(token);

        assertThat(lido).isPresent();
        assertThat(lido.get().id()).isEqualTo(user.getId());
        assertThat(lido.get().email()).isEqualTo("felix@ufac.br");
        assertThat(lido.get().name()).isEqualTo("Felix");
    }

    @Test
    void token_adulterado_e_recusado() {
        String token = service.issueAccessToken(new User("Felix", "felix@ufac.br", "hash"));
        String adulterado = token.substring(0, token.length() - 2) + "xy";

        assertThat(service.readAccessToken(adulterado)).isEmpty();
    }

    @Test
    void token_assinado_com_outra_chave_e_recusado() {
        var outroServico = new JwtService(
                new JwtProperties(null, Duration.ofMinutes(15), Duration.ofDays(7)));
        String tokenDeOutraInstancia =
                outroServico.issueAccessToken(new User("Felix", "felix@ufac.br", "hash"));

        assertThat(service.readAccessToken(tokenDeOutraInstancia)).isEmpty();
    }

    @Test
    void token_expirado_e_recusado() {
        var expirado = new JwtService(
                new JwtProperties(null, Duration.ofSeconds(-1), Duration.ofDays(7)));
        String token = expirado.issueAccessToken(new User("Felix", "felix@ufac.br", "hash"));

        assertThat(expirado.readAccessToken(token)).isEmpty();
    }
}
