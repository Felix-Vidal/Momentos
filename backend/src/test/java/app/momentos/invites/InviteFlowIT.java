package app.momentos.invites;

import static org.assertj.core.api.Assertions.assertThat;

import app.momentos.support.ApiTest;
import app.momentos.support.IntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@IntegrationTest
class InviteFlowIT extends ApiTest {

    private Session admin;
    private Session convidado;
    private UUID grupoId;

    @BeforeEach
    void cenario() {
        cleanDatabase();
        admin = register("Felix", "felix@ufac.br");
        convidado = register("Ana", "ana@ufac.br");
        grupoId = createGroup(admin, "Sociedade da Roleta", "PRIVATE");
    }

    @Test
    void link_de_convite_coloca_a_pessoa_no_grupo() {
        String token = criarConvite(null, null, null);

        // Antes de aceitar, o convite mostra apenas de qual grupo é.
        api(convidado.accessToken()).get().uri("/api/invites/" + token)
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.groupName").isEqualTo("Sociedade da Roleta")
                .jsonPath("$.invitedBy").isEqualTo("Felix")
                .jsonPath("$.valid").isEqualTo(true)
                .jsonPath("$.alreadyMember").isEqualTo(false);

        api(convidado.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.groupId").isEqualTo(grupoId.toString());

        // Agora o grupo privado abre para quem entrou.
        api(convidado.accessToken()).get().uri("/api/groups/" + grupoId)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.myRole").isEqualTo("MEMBER");
    }

    @Test
    void convite_de_uso_unico_nao_serve_para_a_segunda_pessoa() {
        var terceiro = register("Bruno", "bruno@ufac.br");
        String token = criarConvite(null, null, 1);

        api(convidado.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isOk();

        api(terceiro.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isEqualTo(410)
                .expectBody().jsonPath("$.code").isEqualTo("convite_invalido");
    }

    @Test
    void link_com_varios_usos_aceita_mais_de_uma_pessoa() {
        var terceiro = register("Bruno", "bruno@ufac.br");
        String token = criarConvite(null, null, 5);

        api(convidado.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isOk();
        api(terceiro.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isOk();

        api(admin.accessToken()).get().uri("/api/groups/" + grupoId + "/members")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(3);
    }

    @Test
    void convite_revogado_deixa_de_valer() {
        String token = criarConvite(null, null, 5);

        var convites = api(admin.accessToken()).get().uri("/api/groups/" + grupoId + "/invites")
                .exchange().expectStatus().isOk()
                .expectBody(InviteResponse[].class).returnResult().getResponseBody();
        // A listagem nunca devolve o token em claro.
        assertThat(convites).hasSize(1);
        assertThat(convites[0].token()).isNull();

        api(admin.accessToken()).delete()
                .uri("/api/groups/" + grupoId + "/invites/" + convites[0].id())
                .exchange().expectStatus().isNoContent();

        api(convidado.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isEqualTo(410)
                .expectBody().jsonPath("$.message")
                .value((String m) -> assertThat(m).contains("revogado"));
    }

    @Test
    void convite_dirigido_a_uma_pessoa_nao_serve_para_outra() {
        var outro = register("Bruno", "bruno@ufac.br");
        String token = criarConvite("ana@ufac.br", null, null);

        api(outro.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isEqualTo(410)
                .expectBody().jsonPath("$.message")
                .value((String m) -> assertThat(m).contains("outra pessoa"));

        api(convidado.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isOk();
    }

    @Test
    void convite_dirigido_aparece_para_quem_foi_convidado() {
        criarConvite("ana@ufac.br", null, null);

        api(convidado.accessToken()).get().uri("/api/invites/mine")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].invitedEmail").isEqualTo("ana@ufac.br");
    }

    @Test
    void convite_expirado_nao_vale() {
        // Não é possível criar com validade negativa pela API; o cenário é montado no banco.
        String token = criarConvite(null, null, 5);
        jdbc.update("update group_invites set expires_at = now() - interval '1 hour'");

        api(convidado.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isEqualTo(410)
                .expectBody().jsonPath("$.message")
                .value((String m) -> assertThat(m).contains("expirou"));
    }

    @Test
    void so_administrador_gera_convite() {
        String token = criarConvite(null, null, 5);
        api(convidado.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isOk();

        api(convidado.accessToken()).post().uri("/api/groups/" + grupoId + "/invites")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new InvitePayload(null, null, null, null))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void convite_pode_promover_direto_a_administrador() {
        String token = criarConvite(null, "ADMIN", null);

        api(convidado.accessToken()).post().uri("/api/invites/" + token + "/accept")
                .exchange().expectStatus().isOk();

        api(convidado.accessToken()).get().uri("/api/groups/" + grupoId)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.myRole").isEqualTo("ADMIN");
    }

    @Test
    void token_inventado_nao_revela_nada() {
        api(convidado.accessToken()).get().uri("/api/invites/token-que-nao-existe")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.valid").isEqualTo(false)
                .jsonPath("$.groupName").doesNotExist();
    }

    @Test
    void solicitacao_de_entrada_depende_de_aprovacao() {
        UUID aberto = createGroup(admin, "Grupo Aberto", "OPEN");

        api(convidado.accessToken()).post().uri("/api/groups/" + aberto + "/join-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MessagePayload("Quero participar!"))
                .exchange().expectStatus().isCreated();

        // Enquanto não aprovam, o conteúdo segue fechado.
        api(convidado.accessToken()).get().uri("/api/groups/" + aberto + "/timeline")
                .exchange().expectStatus().isForbidden();

        var pedidos = api(admin.accessToken()).get()
                .uri("/api/groups/" + aberto + "/join-requests")
                .exchange().expectStatus().isOk()
                .expectBody(JoinRequestResponse[].class).returnResult().getResponseBody();
        assertThat(pedidos).hasSize(1);
        assertThat(pedidos[0].userName()).isEqualTo("Ana");
        assertThat(pedidos[0].message()).isEqualTo("Quero participar!");

        api(admin.accessToken()).post()
                .uri("/api/groups/" + aberto + "/join-requests/" + pedidos[0].id() + "/approve")
                .exchange().expectStatus().isNoContent();

        api(convidado.accessToken()).get().uri("/api/groups/" + aberto + "/timeline")
                .exchange().expectStatus().isOk();
    }

    @Test
    void pedido_recusado_nao_coloca_a_pessoa_no_grupo() {
        UUID aberto = createGroup(admin, "Grupo Aberto", "OPEN");
        pedirEntrada(aberto);

        var pedidos = api(admin.accessToken()).get()
                .uri("/api/groups/" + aberto + "/join-requests")
                .exchange().expectStatus().isOk()
                .expectBody(JoinRequestResponse[].class).returnResult().getResponseBody();

        api(admin.accessToken()).post()
                .uri("/api/groups/" + aberto + "/join-requests/" + pedidos[0].id() + "/reject")
                .exchange().expectStatus().isNoContent();

        api(convidado.accessToken()).get().uri("/api/groups/" + aberto)
                .exchange().expectStatus().isForbidden();

        // E a lista de pendentes fica vazia.
        api(admin.accessToken()).get().uri("/api/groups/" + aberto + "/join-requests")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void nao_e_possivel_pedir_duas_vezes_nem_pedir_para_grupo_privado() {
        UUID aberto = createGroup(admin, "Grupo Aberto", "OPEN");
        pedirEntrada(aberto);

        api(convidado.accessToken()).post().uri("/api/groups/" + aberto + "/join-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MessagePayload(null))
                .exchange().expectStatus().isEqualTo(409);

        // Grupo privado se comporta como inexistente para quem está fora.
        api(convidado.accessToken()).post().uri("/api/groups/" + grupoId + "/join-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MessagePayload(null))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void a_vitrine_marca_quando_ja_existe_pedido_pendente() {
        UUID aberto = createGroup(admin, "Grupo Aberto", "OPEN");
        pedirEntrada(aberto);

        api(convidado.accessToken()).get().uri("/api/groups/public?q=Aberto")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].requested").isEqualTo(true)
                .jsonPath("$.content[0].member").isEqualTo(false);
    }

    @Test
    void quem_pediu_pode_cancelar_o_proprio_pedido() {
        UUID aberto = createGroup(admin, "Grupo Aberto", "OPEN");
        pedirEntrada(aberto);

        api(convidado.accessToken()).delete()
                .uri("/api/groups/" + aberto + "/join-requests/mine")
                .exchange().expectStatus().isNoContent();

        api(admin.accessToken()).get().uri("/api/groups/" + aberto + "/join-requests")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void membro_comum_nao_ve_nem_responde_pedidos() {
        UUID aberto = createGroup(admin, "Grupo Aberto", "OPEN");
        var terceiro = register("Bruno", "bruno@ufac.br");
        addMember(aberto, terceiro, "MEMBER");
        pedirEntrada(aberto);

        api(terceiro.accessToken()).get().uri("/api/groups/" + aberto + "/join-requests")
                .exchange().expectStatus().isForbidden();
    }

    // --- utilidades ---

    private String criarConvite(String email, String role, Integer maxUses) {
        return api(admin.accessToken()).post().uri("/api/groups/" + grupoId + "/invites")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new InvitePayload(email, role, null, maxUses))
                .exchange().expectStatus().isCreated()
                .expectBody(InviteResponse.class).returnResult().getResponseBody().token();
    }

    private void pedirEntrada(UUID groupId) {
        api(convidado.accessToken()).post().uri("/api/groups/" + groupId + "/join-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MessagePayload("Quero participar!"))
                .exchange().expectStatus().isCreated();
    }

    record InvitePayload(String email, String role, Integer expiresInHours, Integer maxUses) {
    }

    record MessagePayload(String message) {
    }

    record InviteResponse(String id, String token, String invitedEmail, String role, int maxUses,
            int uses, boolean revoked) {
    }

    record JoinRequestResponse(String id, String userId, String userName, String message) {
    }
}
