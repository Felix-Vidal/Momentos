package app.momentos.categories;

import static org.assertj.core.api.Assertions.assertThat;

import app.momentos.media.processing.ExifFixtures;
import app.momentos.support.ApiTest;
import app.momentos.support.IntegrationTest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;

@IntegrationTest
class CategoriesIT extends ApiTest {

    private static final ZoneId ACRE = ZoneId.of("America/Rio_Branco");

    private Session admin;
    private Session membro;
    private UUID grupoId;

    @BeforeEach
    void cenario() {
        cleanDatabase();
        admin = register("Felix", "felix@ufac.br");
        membro = register("Ana", "ana@ufac.br");
        grupoId = createGroup(admin, "Sociedade da Roleta", "OPEN");
        addMember(grupoId, membro, "MEMBER");
    }

    @Test
    void qualquer_membro_cria_as_categorias_do_grupo() {
        api(membro.accessToken()).post().uri("/api/groups/" + grupoId + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload("Tereré", "#34d399", "🧉"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Tereré")
                .jsonPath("$.color").isEqualTo("#34d399")
                .jsonPath("$.emoji").isEqualTo("🧉")
                .jsonPath("$.mediaCount").isEqualTo(0);
    }

    @Test
    void nome_repetido_no_mesmo_grupo_e_recusado_mesmo_com_acento_ou_caixa_diferente() {
        criarCategoria("Almoço", "#f5b53f", "🍽");

        api(admin.accessToken()).post().uri("/api/groups/" + grupoId + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload("ALMOCO", null, null))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("conflito");
    }

    @Test
    void grupos_diferentes_tem_vocabularios_independentes() {
        UUID outroGrupo = createGroup(admin, "Turma de SI", "PRIVATE");
        criarCategoria("Jogos", "#a855f7", "🎲");

        // O mesmo nome é permitido em outro grupo.
        api(admin.accessToken()).post().uri("/api/groups/" + outroGrupo + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload("Jogos", null, null))
                .exchange().expectStatus().isCreated();

        api(admin.accessToken()).get().uri("/api/groups/" + grupoId + "/categories")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void cor_invalida_e_recusada() {
        api(admin.accessToken()).post().uri("/api/groups/" + grupoId + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload("Festa", "vermelho", null))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.fields[0].field").isEqualTo("color");
    }

    @Test
    void uma_midia_pode_ter_varias_categorias_e_a_lista_substitui_a_anterior() {
        String foto = enviarFoto("roleta.jpg", LocalDateTime.of(2026, 10, 13, 12, 30));
        String almoco = criarCategoria("Almoço", "#f5b53f", "🍽");
        String roleta = criarCategoria("Roleta", "#a855f7", "🎯");
        String jogos = criarCategoria("Jogos", "#34d399", "🎲");

        marcar(foto, List.of(almoco, roleta));
        api(admin.accessToken()).get().uri("/api/media/" + foto + "/categories?groupId=" + grupoId)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2);

        // A lista enviada é o estado final: "almoço" sai, "jogos" entra.
        marcar(foto, List.of(roleta, jogos));
        var atuais = api(admin.accessToken())
                .get().uri("/api/media/" + foto + "/categories?groupId=" + grupoId)
                .exchange().expectStatus().isOk()
                .expectBody(CategoryResponse[].class).returnResult().getResponseBody();

        assertThat(atuais).extracting(CategoryResponse::name)
                .containsExactlyInAnyOrder("Roleta", "Jogos");
    }

    @Test
    void a_contagem_por_categoria_acompanha_as_marcacoes() {
        String f1 = enviarFoto("a.jpg", LocalDateTime.of(2026, 10, 13, 12, 30));
        String f2 = enviarFoto("b.jpg", LocalDateTime.of(2026, 10, 13, 12, 40));
        String almoco = criarCategoria("Almoço", "#f5b53f", "🍽");
        marcar(f1, List.of(almoco));
        marcar(f2, List.of(almoco));

        api(admin.accessToken()).get().uri("/api/groups/" + grupoId + "/categories")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].name").isEqualTo("Almoço")
                .jsonPath("$[0].mediaCount").isEqualTo(2);
    }

    @Test
    void os_selos_vem_junto_na_linha_do_tempo() {
        String foto = enviarFoto("roleta.jpg", LocalDateTime.of(2026, 10, 13, 12, 30));
        String roleta = criarCategoria("Roleta", "#a855f7", "🎯");
        marcar(foto, List.of(roleta));

        api(admin.accessToken()).get().uri("/api/groups/" + grupoId + "/timeline")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items[0].media.categories.length()").isEqualTo(1)
                .jsonPath("$.items[0].media.categories[0].name").isEqualTo("Roleta")
                .jsonPath("$.items[0].media.categories[0].emoji").isEqualTo("🎯");
    }

    @Test
    void a_linha_do_tempo_pode_ser_filtrada_por_categoria() {
        String comRoleta = enviarFoto("roleta.jpg", LocalDateTime.of(2026, 10, 13, 12, 30));
        enviarFoto("sem-categoria.jpg", LocalDateTime.of(2026, 10, 13, 15, 0));
        String roleta = criarCategoria("Roleta", "#a855f7", "🎯");
        marcar(comRoleta, List.of(roleta));

        api(admin.accessToken()).get().uri("/api/groups/" + grupoId + "/timeline")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.items.length()").isEqualTo(2);

        api(admin.accessToken())
                .get().uri("/api/groups/" + grupoId + "/timeline?categoryIds=" + roleta)
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].media.media.id").isEqualTo(comRoleta);
    }

    @Test
    void com_filtro_ativo_momento_sem_midia_correspondente_some_da_pagina() {
        String comCategoria = enviarFoto("corujao.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        enviarFoto("almoco.jpg", LocalDateTime.of(2026, 10, 13, 12, 30));
        criarMomento("Almoço no RU",
                LocalDateTime.of(2026, 10, 13, 11, 30), LocalDateTime.of(2026, 10, 13, 14, 0));
        criarMomento("Corujão da LetalCompany",
                LocalDateTime.of(2026, 10, 13, 22, 0), LocalDateTime.of(2026, 10, 14, 6, 0));
        String jogos = criarCategoria("Jogos", "#34d399", "🎲");
        marcar(comCategoria, List.of(jogos));

        api(admin.accessToken())
                .get().uri("/api/groups/" + grupoId + "/timeline?categoryIds=" + jogos)
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].moment.moment.title").isEqualTo("Corujão da LetalCompany");
    }

    @Test
    void categoria_de_outro_grupo_nao_pode_marcar_a_midia() {
        UUID outroGrupo = createGroup(admin, "Turma de SI", "PRIVATE");
        String categoriaDeOutroGrupo = api(admin.accessToken())
                .post().uri("/api/groups/" + outroGrupo + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload("Trabalho", null, null))
                .exchange().expectStatus().isCreated()
                .expectBody(CategoryResponse.class).returnResult().getResponseBody().id();

        String foto = enviarFoto("roleta.jpg", LocalDateTime.of(2026, 10, 13, 12, 30));

        api(admin.accessToken()).put().uri("/api/media/" + foto + "/categories?groupId=" + grupoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SetPayload(List.of(categoriaDeOutroGrupo)))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void quem_nao_e_do_grupo_nao_ve_nem_cria_categorias() {
        var estranho = register("Visitante", "visita@ufac.br");

        api(estranho.accessToken()).get().uri("/api/groups/" + grupoId + "/categories")
                .exchange().expectStatus().isForbidden();

        api(estranho.accessToken()).post().uri("/api/groups/" + grupoId + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload("Invasão", null, null))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void autor_e_administrador_editam_a_categoria_mas_outro_membro_nao() {
        String daAna = api(membro.accessToken()).post()
                .uri("/api/groups/" + grupoId + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload("Tereré", "#34d399", "🧉"))
                .exchange().expectStatus().isCreated()
                .expectBody(CategoryResponse.class).returnResult().getResponseBody().id();

        // A autora edita.
        api(membro.accessToken()).patch().uri("/api/categories/" + daAna)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload("Tereré da tarde", null, null))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Tereré da tarde");

        // O administrador também.
        api(admin.accessToken()).patch().uri("/api/categories/" + daAna)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload(null, "#f43f5e", null))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.color").isEqualTo("#f43f5e");

        // Um terceiro membro comum, não.
        var terceiro = register("Bruno", "bruno@ufac.br");
        addMember(grupoId, terceiro, "MEMBER");
        api(terceiro.accessToken()).patch().uri("/api/categories/" + daAna)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload("Outro nome", null, null))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void apagar_a_categoria_tira_a_etiqueta_sem_apagar_a_midia() {
        String foto = enviarFoto("roleta.jpg", LocalDateTime.of(2026, 10, 13, 12, 30));
        String roleta = criarCategoria("Roleta", "#a855f7", "🎯");
        marcar(foto, List.of(roleta));

        api(admin.accessToken()).delete().uri("/api/categories/" + roleta)
                .exchange().expectStatus().isNoContent();

        api(admin.accessToken()).get().uri("/api/groups/" + grupoId + "/timeline")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].media.categories.length()").isEqualTo(0);
    }

    // --- utilidades ---

    private String criarCategoria(String nome, String cor, String emoji) {
        return api(admin.accessToken()).post().uri("/api/groups/" + grupoId + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryPayload(nome, cor, emoji))
                .exchange().expectStatus().isCreated()
                .expectBody(CategoryResponse.class).returnResult().getResponseBody().id();
    }

    private void marcar(String mediaId, List<String> categoryIds) {
        api(admin.accessToken()).put().uri("/api/media/" + mediaId + "/categories?groupId=" + grupoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SetPayload(categoryIds))
                .exchange().expectStatus().isOk();
    }

    private String criarMomento(String titulo, LocalDateTime inicio, LocalDateTime fim) {
        return api(admin.accessToken()).post().uri("/api/groups/" + grupoId + "/moments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MomentPayload(titulo, inicio.atZone(ACRE).toInstant().toString(),
                        fim.atZone(ACRE).toInstant().toString()))
                .exchange().expectStatus().isCreated()
                .expectBody(IdResponse.class).returnResult().getResponseBody().id();
    }

    private String enviarFoto(String nome, LocalDateTime capturadaEm) {
        try {
            var builder = new MultipartBodyBuilder();
            builder.part("files", new ByteArrayResource(
                            ExifFixtures.jpegWithCaptureDate(capturadaEm)))
                    .filename(nome).contentType(MediaType.IMAGE_JPEG);
            return api(admin.accessToken()).post()
                    .uri("/api/media?groupIds=" + grupoId + "&timeZone=America/Rio_Branco")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .exchange().expectStatus().isOk()
                    .expectBody(UploadResponse.class).returnResult().getResponseBody()
                    .uploaded().getFirst().id();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    record CategoryPayload(String name, String color, String emoji) {
    }

    record SetPayload(List<String> categoryIds) {
    }

    record MomentPayload(String title, String startsAt, String endsAt) {
    }

    record CategoryResponse(String id, String name, String color, String emoji, long mediaCount) {
    }

    record IdResponse(String id) {
    }

    record UploadResponse(List<IdResponse> uploaded) {
    }
}
