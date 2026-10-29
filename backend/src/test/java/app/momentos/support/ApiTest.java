package app.momentos.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Base dos testes que conversam com a API pela rede, com atalhos para registrar
 * usuários e autenticar requisições.
 */
public abstract class ApiTest {

    @LocalServerPort
    protected int port;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    protected RestTestClient api() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    protected RestTestClient api(String accessToken) {
        return RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    /** Cria uma conta e devolve a sessão recém-aberta. */
    protected Session register(String name, String email) {
        var body = api().post()
                .uri("/api/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new RegisterPayload(name, email, "senhaSegura123"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SessionPayload.class)
                .returnResult()
                .getResponseBody();

        return new Session(body.accessToken(), body.refreshToken(),
                java.util.UUID.fromString(body.user().id()));
    }

    protected void cleanDatabase() {
        jdbc.execute("truncate table users cascade");
    }

    public record RegisterPayload(String name, String email, String password) {
    }

    public record SessionPayload(String accessToken, String refreshToken, long expiresIn,
            UserPayload user) {
    }

    public record UserPayload(String id, String name, String email, String bio) {
    }

    public record Session(String accessToken, String refreshToken, java.util.UUID userId) {
    }
}
