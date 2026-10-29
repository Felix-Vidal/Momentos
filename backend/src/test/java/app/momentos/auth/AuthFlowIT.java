package app.momentos.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.momentos.support.ApiTest;
import app.momentos.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@IntegrationTest
class AuthFlowIT extends ApiTest {

    @BeforeEach
    void limpar() {
        cleanDatabase();
    }

    @Test
    void cadastro_login_e_acesso_autenticado() {
        var sessao = register("Felix", "felix@ufac.br");
        assertThat(sessao.accessToken()).isNotBlank();
        assertThat(sessao.refreshToken()).isNotBlank();

        // O perfil autenticado inclui o e-mail do próprio usuário.
        api(sessao.accessToken()).get().uri("/api/users/me").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Felix")
                .jsonPath("$.email").isEqualTo("felix@ufac.br");

        // Login com as mesmas credenciais abre outra sessão.
        api().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginPayload("felix@ufac.br", "senhaSegura123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.accessToken").exists();
    }

    @Test
    void sem_token_a_api_responde_401_com_corpo_de_erro() {
        api().get().uri("/api/users/me").exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("nao_autenticado")
                .jsonPath("$.message").isEqualTo("Faça login para continuar.");
    }

    @Test
    void email_repetido_e_recusado() {
        register("Felix", "felix@ufac.br");

        api().post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterPayload("Outro", "FELIX@ufac.br", "senhaSegura123"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("conflito");
    }

    @Test
    void senha_errada_nao_abre_sessao() {
        register("Felix", "felix@ufac.br");

        api().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginPayload("felix@ufac.br", "senhaErrada123"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo("credenciais_invalidas");
    }

    @Test
    void dados_invalidos_voltam_com_erros_por_campo() {
        api().post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterPayload("F", "nao-e-email", "123"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("dados_invalidos")
                .jsonPath("$.fields.length()").isEqualTo(3);
    }

    @Test
    void refresh_rotativo_invalida_o_token_anterior() {
        var sessao = register("Felix", "felix@ufac.br");

        var renovada = api().post().uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshPayload(sessao.refreshToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(SessionPayload.class)
                .returnResult()
                .getResponseBody();

        assertThat(renovada.refreshToken()).isNotEqualTo(sessao.refreshToken());

        // Reusar o token antigo não vale: a rotação o revogou.
        api().post().uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshPayload(sessao.refreshToken()))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo("sessao_invalida");

        // O token novo continua funcionando.
        api(renovada.accessToken()).get().uri("/api/users/me").exchange().expectStatus().isOk();
    }

    @Test
    void logout_revoga_a_sessao() {
        var sessao = register("Felix", "felix@ufac.br");

        api().post().uri("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshPayload(sessao.refreshToken()))
                .exchange()
                .expectStatus().isNoContent();

        api().post().uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshPayload(sessao.refreshToken()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void perfil_pode_ser_atualizado_e_o_publico_nao_expoe_email() {
        var sessao = register("Felix", "felix@ufac.br");

        api(sessao.accessToken()).patch().uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdatePayload("Felix Vidal", "Girando a roleta desde 2024."))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Felix Vidal");

        api(sessao.accessToken()).get().uri("/api/users/" + sessao.userId()).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Felix Vidal")
                .jsonPath("$.bio").isEqualTo("Girando a roleta desde 2024.")
                .jsonPath("$.email").doesNotExist();
    }

    record LoginPayload(String email, String password) {
    }

    record RefreshPayload(String refreshToken) {
    }

    record UpdatePayload(String name, String bio) {
    }
}
