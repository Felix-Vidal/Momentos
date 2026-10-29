package app.momentos.comments;

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
class CommentsIT extends ApiTest {

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
    void membros_comentam_em_uma_foto_e_o_autor_aparece_no_comentario() {
        String fotoId = enviarFoto("foto.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));

        comentarNaMidia(membro, fotoId, "Essa foi a rodada que decidiu tudo");

        api(admin.accessToken()).get()
                .uri("/api/media/" + fotoId + "/comments?groupId=" + grupoId)
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].body").isEqualTo("Essa foi a rodada que decidiu tudo")
                .jsonPath("$[0].author.name").isEqualTo("Ana")
                // Administrador pode apagar comentário de outro, mas não editar.
                .jsonPath("$[0].canDelete").isEqualTo(true)
                .jsonPath("$[0].canEdit").isEqualTo(false);
    }

    @Test
    void autor_edita_o_proprio_comentario_e_ele_fica_marcado_como_editado() {
        String fotoId = enviarFoto("foto.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        String comentarioId = comentarNaMidia(membro, fotoId, "Texto original");

        api(membro.accessToken()).patch().uri("/api/comments/" + comentarioId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CommentPayload("Texto corrigido"))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.body").isEqualTo("Texto corrigido");

        api(admin.accessToken()).patch().uri("/api/comments/" + comentarioId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CommentPayload("Nem administrador edita texto alheio"))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void administrador_apaga_qualquer_comentario_e_o_autor_apaga_o_seu() {
        String fotoId = enviarFoto("foto.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        String doMembro = comentarNaMidia(membro, fotoId, "Comentário da Ana");
        String doAdmin = comentarNaMidia(admin, fotoId, "Comentário do Felix");

        api(admin.accessToken()).delete().uri("/api/comments/" + doMembro)
                .exchange().expectStatus().isNoContent();
        api(admin.accessToken()).delete().uri("/api/comments/" + doAdmin)
                .exchange().expectStatus().isNoContent();

        api(admin.accessToken()).get().uri("/api/media/" + fotoId + "/comments?groupId=" + grupoId)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void membro_comum_nao_apaga_comentario_de_outro() {
        String fotoId = enviarFoto("foto.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        String doAdmin = comentarNaMidia(admin, fotoId, "Comentário do Felix");

        api(membro.accessToken()).delete().uri("/api/comments/" + doAdmin)
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void quem_nao_e_do_grupo_nao_le_nem_escreve_comentarios() {
        var estranho = register("Visitante", "visita@ufac.br");
        String fotoId = enviarFoto("foto.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));

        api(estranho.accessToken()).get()
                .uri("/api/media/" + fotoId + "/comments?groupId=" + grupoId)
                .exchange().expectStatus().isForbidden();

        api(estranho.accessToken()).post()
                .uri("/api/media/" + fotoId + "/comments?groupId=" + grupoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CommentPayload("deixa eu comentar"))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void a_mesma_foto_em_dois_grupos_tem_conversas_separadas() {
        String fotoId = enviarFoto("foto.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        UUID outroGrupo = createGroup(admin, "Turma de SI", "PRIVATE");

        api(admin.accessToken()).post().uri("/api/media/" + fotoId + "/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SharePayload(List.of(outroGrupo.toString())))
                .exchange().expectStatus().isNoContent();

        comentarNaMidia(admin, fotoId, "Comentário na Sociedade da Roleta");

        // O outro grupo vê a foto, mas não a conversa do primeiro.
        api(admin.accessToken()).get()
                .uri("/api/media/" + fotoId + "/comments?groupId=" + outroGrupo)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(0);

        api(admin.accessToken()).get()
                .uri("/api/media/" + fotoId + "/comments?groupId=" + grupoId)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void comentario_em_midia_que_nao_esta_no_grupo_e_recusado() {
        UUID outroGrupo = createGroup(admin, "Turma de SI", "PRIVATE");
        String fotoId = enviarFoto("foto.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));

        api(admin.accessToken()).post()
                .uri("/api/media/" + fotoId + "/comments?groupId=" + outroGrupo)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CommentPayload("essa foto não está aqui"))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void o_feed_do_momento_reune_os_comentarios_do_conjunto_com_a_midia_de_cada_um() {
        String foto1 = enviarFoto("corujao-1.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        String foto2 = enviarFoto("corujao-2.jpg", LocalDateTime.of(2026, 10, 14, 2, 0));
        String foraDoMomento = enviarFoto("dia-seguinte.jpg", LocalDateTime.of(2026, 10, 14, 15, 0));

        String momentoId = criarMomento("Corujão da LetalCompany",
                LocalDateTime.of(2026, 10, 13, 22, 0), LocalDateTime.of(2026, 10, 14, 6, 0));

        comentarNaMidia(membro, foto1, "Comentário da primeira foto");
        comentarNaMidia(admin, foto2, "Comentário da segunda foto");
        comentarNaMidia(admin, foraDoMomento, "Esse não é do corujão");
        comentarNoMomento(membro, momentoId, "Que madrugada memorável");

        var feed = api(admin.accessToken()).get().uri("/api/moments/" + momentoId + "/comments")
                .exchange().expectStatus().isOk()
                .expectBody(CommentResponse[].class).returnResult().getResponseBody();

        assertThat(feed).hasSize(3);
        assertThat(feed).extracting(CommentResponse::body)
                .containsExactly("Comentário da primeira foto", "Comentário da segunda foto",
                        "Que madrugada memorável");
        // Cada comentário de mídia leva a miniatura da foto a que se refere;
        // o do próprio Momento não tem mídia.
        assertThat(feed[0].mediaThumbnail()).isEqualTo(foto1);
        assertThat(feed[1].mediaThumbnail()).isEqualTo(foto2);
        assertThat(feed[2].mediaThumbnail()).isNull();
        assertThat(feed[2].momentId()).isEqualTo(momentoId);
    }

    @Test
    void a_linha_do_tempo_ja_vem_com_os_comentarios_de_cada_midia_e_do_momento() {
        String foto = enviarFoto("corujao.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        String momentoId = criarMomento("Corujão da LetalCompany",
                LocalDateTime.of(2026, 10, 13, 22, 0), LocalDateTime.of(2026, 10, 14, 6, 0));
        comentarNaMidia(membro, foto, "Primeiro comentário");
        comentarNaMidia(admin, foto, "Segundo comentário");
        comentarNoMomento(admin, momentoId, "Comentário do momento");

        api(admin.accessToken()).get().uri("/api/groups/" + grupoId + "/timeline")
                .exchange().expectStatus().isOk()
                .expectBody()
                // A caixa animada da mídia abre sem precisar de outra chamada.
                .jsonPath("$.items[0].moment.media[0].commentCount").isEqualTo(2)
                .jsonPath("$.items[0].moment.media[0].comments.length()").isEqualTo(2)
                .jsonPath("$.items[0].moment.media[0].comments[0].author.name").isEqualTo("Ana")
                // E o feed do título percorre o conjunto inteiro.
                .jsonPath("$.items[0].moment.commentCount").isEqualTo(3)
                .jsonPath("$.items[0].moment.commentFeed.length()").isEqualTo(3);
    }

    @Test
    void comentario_vazio_ou_longo_demais_e_recusado() {
        String fotoId = enviarFoto("foto.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));

        api(membro.accessToken()).post()
                .uri("/api/media/" + fotoId + "/comments?groupId=" + grupoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CommentPayload("   "))
                .exchange().expectStatus().isBadRequest();

        api(membro.accessToken()).post()
                .uri("/api/media/" + fotoId + "/comments?groupId=" + grupoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CommentPayload("x".repeat(1001)))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void apagar_a_midia_leva_os_comentarios_dela() {
        String fotoId = enviarFoto("foto.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        comentarNaMidia(membro, fotoId, "Comentário que vai junto");

        api(admin.accessToken()).delete().uri("/api/media/" + fotoId)
                .exchange().expectStatus().isNoContent();

        api(admin.accessToken()).get().uri("/api/groups/" + grupoId + "/timeline")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.items.length()").isEqualTo(0);
    }

    // --- utilidades ---

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

    private String criarMomento(String titulo, LocalDateTime inicio, LocalDateTime fim) {
        return api(admin.accessToken()).post().uri("/api/groups/" + grupoId + "/moments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MomentPayload(titulo, inicio.atZone(ACRE).toInstant().toString(),
                        fim.atZone(ACRE).toInstant().toString()))
                .exchange().expectStatus().isCreated()
                .expectBody(IdResponse.class).returnResult().getResponseBody().id();
    }

    private String comentarNaMidia(Session user, String mediaId, String texto) {
        return api(user.accessToken()).post()
                .uri("/api/media/" + mediaId + "/comments?groupId=" + grupoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CommentPayload(texto))
                .exchange().expectStatus().isCreated()
                .expectBody(CommentResponse.class).returnResult().getResponseBody().id();
    }

    private String comentarNoMomento(Session user, String momentId, String texto) {
        return api(user.accessToken()).post().uri("/api/moments/" + momentId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CommentPayload(texto))
                .exchange().expectStatus().isCreated()
                .expectBody(CommentResponse.class).returnResult().getResponseBody().id();
    }

    record CommentPayload(String body) {
    }

    record MomentPayload(String title, String startsAt, String endsAt) {
    }

    record SharePayload(List<String> groupIds) {
    }

    record IdResponse(String id) {
    }

    record CommentResponse(String id, String body, String mediaId, String momentId,
            String mediaThumbnail, AuthorResponse author, boolean canEdit, boolean canDelete) {
    }

    record AuthorResponse(String id, String name) {
    }

    record UploadResponse(List<IdResponse> uploaded) {
    }
}
