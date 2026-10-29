package app.momentos;

import static org.assertj.core.api.Assertions.assertThat;

import app.momentos.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Verifica a fundação: contexto sobe, Flyway aplica as migrações e a API responde. */
@IntegrationTest
class FundacaoIT {

    @LocalServerPort
    private int porta;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void api_responde_ao_ping() {
        RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + porta)
                .build()
                .get()
                .uri("/api/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok");
    }

    @Test
    void flyway_aplicou_a_migracao_baseline() {
        Integer aplicadas = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class);
        assertThat(aplicadas).isGreaterThanOrEqualTo(1);
    }

    @Test
    void funcao_de_busca_sem_acento_esta_disponivel() {
        String resultado = jdbc.queryForObject(
                "select momentos_slugify_search('Sociedade da Roleta Ação')", String.class);
        assertThat(resultado).isEqualTo("sociedade da roleta acao");
    }
}
