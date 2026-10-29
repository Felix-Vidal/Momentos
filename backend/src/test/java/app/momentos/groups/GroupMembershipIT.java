package app.momentos.groups;

import static org.assertj.core.api.Assertions.assertThat;

import app.momentos.support.ApiTest;
import app.momentos.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@IntegrationTest
class GroupMembershipIT extends ApiTest {

    @BeforeEach
    void limpar() {
        cleanDatabase();
    }

    @Test
    void quem_cria_o_grupo_vira_administrador_e_unico_membro() {
        var dono = register("Felix", "felix@ufac.br");

        api(dono.accessToken()).post().uri("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GroupPayload("Sociedade da Roleta", "Girando a roleta no RU", "OPEN", "ROULETTE"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.slug").isEqualTo("sociedade-da-roleta")
                .jsonPath("$.myRole").isEqualTo("ADMIN")
                .jsonPath("$.theme").isEqualTo("ROULETTE")
                .jsonPath("$.memberCount").isEqualTo(1);

        api(dono.accessToken()).get().uri("/api/groups").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void nomes_iguais_geram_slugs_distintos() {
        var dono = register("Felix", "felix@ufac.br");
        createGroup(dono, "Sociedade da Roleta", "OPEN");

        api(dono.accessToken()).post().uri("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GroupPayload("Sociedade da Roleta", null, "OPEN", null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.slug").isEqualTo("sociedade-da-roleta-2");
    }

    @Test
    void grupo_aberto_aparece_na_vitrine_publica_mas_o_conteudo_nao() {
        var dono = register("Felix", "felix@ufac.br");
        var estranho = register("Visitante", "visita@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");

        // A vitrine é pública: funciona até sem token.
        api().get().uri("/api/groups/public?q=roleta").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].name").isEqualTo("Sociedade da Roleta")
                .jsonPath("$.content[0].memberCount").isEqualTo(1);

        // Mas o detalhe do grupo exige ser membro.
        api(estranho.accessToken()).get().uri("/api/groups/" + grupoId).exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.code").isEqualTo("acesso_negado");

        api(estranho.accessToken()).get().uri("/api/groups/" + grupoId + "/members").exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void busca_na_vitrine_ignora_acentos_e_caixa() {
        var dono = register("Felix", "felix@ufac.br");
        createGroup(dono, "Corujão da LetalCompany", "OPEN");

        api().get().uri("/api/groups/public?q=corujao").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content.length()").isEqualTo(1);

        api().get().uri("/api/groups/public?q=CORUJÃO").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content.length()").isEqualTo(1);
    }

    @Test
    void grupo_privado_nao_aparece_na_vitrine_e_responde_404_a_estranhos() {
        var dono = register("Felix", "felix@ufac.br");
        var estranho = register("Visitante", "visita@ufac.br");
        var grupoId = createGroup(dono, "Grupo Secreto", "PRIVATE");

        api().get().uri("/api/groups/public?q=secreto").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content.length()").isEqualTo(0);

        api(estranho.accessToken()).get().uri("/api/groups/" + grupoId).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("nao_encontrado");
    }

    @Test
    void somente_administrador_edita_as_configuracoes() {
        var dono = register("Felix", "felix@ufac.br");
        var membro = register("Ana", "ana@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");
        addMember(grupoId, membro, "MEMBER");

        api(membro.accessToken()).patch().uri("/api/groups/" + grupoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GroupPayload("Outro nome", null, null, null))
                .exchange()
                .expectStatus().isForbidden();

        api(dono.accessToken()).patch().uri("/api/groups/" + grupoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GroupPayload("Sociedade da Roleta UFAC", null, "PRIVATE", null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Sociedade da Roleta UFAC")
                .jsonPath("$.visibility").isEqualTo("PRIVATE");
    }

    @Test
    void administrador_promove_e_remove_membros() {
        var dono = register("Felix", "felix@ufac.br");
        var membro = register("Ana", "ana@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");
        addMember(grupoId, membro, "MEMBER");

        api(dono.accessToken()).patch().uri("/api/groups/" + grupoId + "/members/" + membro.userId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RolePayload("ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.role").isEqualTo("ADMIN");

        // Agora que há dois administradores, remover um é permitido.
        api(dono.accessToken()).delete()
                .uri("/api/groups/" + grupoId + "/members/" + membro.userId())
                .exchange()
                .expectStatus().isNoContent();

        var membros = api(dono.accessToken()).get().uri("/api/groups/" + grupoId + "/members")
                .exchange().expectStatus().isOk()
                .expectBody(MemberView[].class).returnResult().getResponseBody();
        assertThat(membros).hasSize(1);
    }

    @Test
    void o_ultimo_administrador_nao_pode_se_rebaixar_nem_sair() {
        var dono = register("Felix", "felix@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");

        api(dono.accessToken()).patch().uri("/api/groups/" + grupoId + "/members/" + dono.userId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RolePayload("MEMBER"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message")
                .value((String mensagem) -> assertThat(mensagem).contains("administrador"));

        api(dono.accessToken()).delete().uri("/api/groups/" + grupoId + "/members/" + dono.userId())
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void membro_comum_pode_sair_do_grupo() {
        var dono = register("Felix", "felix@ufac.br");
        var membro = register("Ana", "ana@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");
        addMember(grupoId, membro, "MEMBER");

        api(membro.accessToken()).delete()
                .uri("/api/groups/" + grupoId + "/members/" + membro.userId())
                .exchange()
                .expectStatus().isNoContent();

        api(membro.accessToken()).get().uri("/api/groups/" + grupoId).exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void grupo_pode_ser_aberto_pelo_slug() {
        var dono = register("Felix", "felix@ufac.br");
        createGroup(dono, "Sociedade da Roleta", "OPEN");

        api(dono.accessToken()).get().uri("/api/groups/by-slug/sociedade-da-roleta").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Sociedade da Roleta");
    }

    record RolePayload(String role) {
    }

    record MemberView(String userId, String name, String role) {
    }
}
