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
    protected org.springframework.jdbc.core.JdbcTemplate jdbc;

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
        jdbc.execute("truncate table users, groups, media cascade");
    }

    /** Cria um grupo e devolve seu id; o criador vira administrador. */
    protected java.util.UUID createGroup(Session owner, String name, String visibility) {
        var body = api(owner.accessToken()).post()
                .uri("/api/groups")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new GroupPayload(name, "Grupo de teste", visibility, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GroupResponse.class)
                .returnResult()
                .getResponseBody();
        return java.util.UUID.fromString(body.id());
    }

    /**
     * Coloca alguém no grupo direto pelo banco. Os fluxos de convite e solicitação têm
     * seus próprios testes; aqui o objetivo é só montar o cenário.
     */
    protected void addMember(java.util.UUID groupId, Session user, String role) {
        jdbc.update("insert into group_members (group_id, user_id, role) values (?, ?, ?)",
                groupId, user.userId(), role);
    }

    public record GroupPayload(String name, String description, String visibility, String theme) {
    }

    public record GroupResponse(String id, String slug, String name, String visibility,
            String theme, String myRole, long memberCount) {
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
