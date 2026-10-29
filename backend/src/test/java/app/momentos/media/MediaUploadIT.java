package app.momentos.media;

import static org.assertj.core.api.Assertions.assertThat;

import app.momentos.media.processing.ExifFixtures;
import app.momentos.support.ApiTest;
import app.momentos.support.IntegrationTest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;

@IntegrationTest
class MediaUploadIT extends ApiTest {

    private static final ZoneId ACRE = ZoneId.of("America/Rio_Branco");

    @BeforeEach
    void limpar() {
        cleanDatabase();
    }

    @Test
    void foto_com_exif_entra_na_data_em_que_foi_tirada() throws Exception {
        var dono = register("Felix", "felix@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");

        var resultado = upload(dono, grupoId,
                "corujao.jpg",
                ExifFixtures.jpegWithCaptureDate(LocalDateTime.of(2026, 10, 13, 22, 30, 0)));

        assertThat(resultado.rejected()).isEmpty();
        assertThat(resultado.uploaded()).hasSize(1);

        var foto = resultado.uploaded().getFirst();
        assertThat(foto.type()).isEqualTo("IMAGE");
        assertThat(foto.capturedAtSource()).isEqualTo("EXIF");
        assertThat(Instant.parse(foto.capturedAt()))
                .isEqualTo(LocalDateTime.of(2026, 10, 13, 22, 30, 0).atZone(ACRE).toInstant());
        // Imagem tem miniatura gerada pelo Thumbnailator.
        assertThat(foto.hasThumbnail()).isTrue();
        assertThat(foto.width()).isEqualTo(200);
        assertThat(foto.height()).isEqualTo(120);
    }

    @Test
    void foto_sem_exif_usa_a_data_do_envio_e_pode_ser_corrigida() throws Exception {
        var dono = register("Felix", "felix@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");

        var foto = upload(dono, grupoId, "sem-exif.jpg", ExifFixtures.jpegWithoutExif(120, 80))
                .uploaded().getFirst();
        assertThat(foto.capturedAtSource()).isEqualTo("UPLOAD");

        var corrigida = Instant.parse("2026-10-13T22:30:00Z");
        api(dono.accessToken()).patch().uri("/api/media/" + foto.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdatePayload(corrigida.toString(), "Primeira rodada"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.capturedAtSource").isEqualTo("MANUAL")
                .jsonPath("$.caption").isEqualTo("Primeira rodada");
    }

    @Test
    void envio_em_lote_aceita_o_que_da_e_recusa_o_resto() throws Exception {
        var dono = register("Felix", "felix@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");

        var builder = new MultipartBodyBuilder();
        builder.part("files", resource(ExifFixtures.jpegWithoutExif(100, 100)))
                .filename("boa.jpg").contentType(MediaType.IMAGE_JPEG);
        builder.part("files", resource(ExifFixtures.jpegWithoutExif(90, 90)))
                .filename("outra.jpg").contentType(MediaType.IMAGE_JPEG);
        builder.part("files", resource("%PDF-1.4 conteudo".getBytes()))
                .filename("documento.pdf").contentType(MediaType.APPLICATION_PDF);

        var resultado = send(dono, grupoId, builder.build());

        assertThat(resultado.uploaded()).hasSize(2);
        assertThat(resultado.rejected()).hasSize(1);
        assertThat(resultado.rejected().getFirst().fileName()).isEqualTo("documento.pdf");
        assertThat(resultado.rejected().getFirst().reason()).contains("fotos e vídeos");
    }

    @Test
    void miniatura_e_arquivo_so_abrem_para_quem_tem_acesso() throws Exception {
        var dono = register("Felix", "felix@ufac.br");
        var membro = register("Ana", "ana@ufac.br");
        var estranho = register("Visitante", "visita@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");
        addMember(grupoId, membro, "MEMBER");

        var foto = upload(dono, grupoId, "foto.jpg", ExifFixtures.jpegWithoutExif(100, 100))
                .uploaded().getFirst();

        // Dono e colega de grupo veem.
        api(dono.accessToken()).get().uri("/api/media/" + foto.id() + "/file")
                .exchange().expectStatus().isOk();
        api(membro.accessToken()).get().uri("/api/media/" + foto.id() + "/thumbnail")
                .exchange().expectStatus().isOk();

        // Quem não participa de nenhum grupo onde a mídia está, não vê.
        api(estranho.accessToken()).get().uri("/api/media/" + foto.id() + "/file")
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void midia_pertence_ao_usuario_e_aparece_em_cada_grupo_escolhido() throws Exception {
        var dono = register("Felix", "felix@ufac.br");
        var grupoA = createGroup(dono, "Sociedade da Roleta", "OPEN");
        var grupoB = createGroup(dono, "Turma de SI", "PRIVATE");

        var foto = upload(dono, grupoA, "foto.jpg", ExifFixtures.jpegWithoutExif(100, 100))
                .uploaded().getFirst();

        api(dono.accessToken()).post().uri("/api/media/" + foto.id() + "/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SharePayload(java.util.List.of(grupoB.toString())))
                .exchange()
                .expectStatus().isNoContent();

        api(dono.accessToken()).get().uri("/api/media/" + foto.id() + "/groups")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2);

        // Retirar de um grupo não apaga a mídia: ela continua sendo do usuário.
        api(dono.accessToken()).delete().uri("/api/media/" + foto.id() + "/groups/" + grupoB)
                .exchange().expectStatus().isNoContent();

        api(dono.accessToken()).get().uri("/api/media/" + foto.id() + "/groups")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void nao_e_possivel_compartilhar_com_grupo_do_qual_nao_participo() throws Exception {
        var dono = register("Felix", "felix@ufac.br");
        var outro = register("Ana", "ana@ufac.br");
        var meuGrupo = createGroup(dono, "Sociedade da Roleta", "OPEN");
        var grupoDaAna = createGroup(outro, "Grupo da Ana", "OPEN");

        var foto = upload(dono, meuGrupo, "foto.jpg", ExifFixtures.jpegWithoutExif(100, 100))
                .uploaded().getFirst();

        api(dono.accessToken()).post().uri("/api/media/" + foto.id() + "/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SharePayload(java.util.List.of(grupoDaAna.toString())))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void so_o_dono_altera_ou_apaga_a_midia() throws Exception {
        var dono = register("Felix", "felix@ufac.br");
        var membro = register("Ana", "ana@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");
        addMember(grupoId, membro, "MEMBER");

        var foto = upload(dono, grupoId, "foto.jpg", ExifFixtures.jpegWithoutExif(100, 100))
                .uploaded().getFirst();

        api(membro.accessToken()).delete().uri("/api/media/" + foto.id())
                .exchange().expectStatus().isForbidden();

        api(dono.accessToken()).delete().uri("/api/media/" + foto.id())
                .exchange().expectStatus().isNoContent();

        api(dono.accessToken()).get().uri("/api/media/" + foto.id() + "/file")
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void administrador_pode_retirar_do_grupo_a_midia_de_outra_pessoa() throws Exception {
        var admin = register("Felix", "felix@ufac.br");
        var membro = register("Ana", "ana@ufac.br");
        var grupoId = createGroup(admin, "Sociedade da Roleta", "OPEN");
        addMember(grupoId, membro, "MEMBER");

        var foto = upload(membro, grupoId, "foto.jpg", ExifFixtures.jpegWithoutExif(100, 100))
                .uploaded().getFirst();

        api(admin.accessToken()).delete().uri("/api/media/" + foto.id() + "/groups/" + grupoId)
                .exchange().expectStatus().isNoContent();

        // A mídia continua existindo para o dono.
        api(membro.accessToken()).get().uri("/api/media/mine")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void foto_propria_pode_virar_avatar_mas_a_de_outro_nao() throws Exception {
        var dono = register("Felix", "felix@ufac.br");
        var outro = register("Ana", "ana@ufac.br");
        var grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");
        addMember(grupoId, outro, "MEMBER");

        var minhaFoto = upload(dono, grupoId, "eu.jpg", ExifFixtures.jpegWithoutExif(100, 100))
                .uploaded().getFirst();

        api(dono.accessToken()).put().uri("/api/users/me/avatar/" + minhaFoto.id())
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.avatarMediaId").isEqualTo(minhaFoto.id());

        api(outro.accessToken()).put().uri("/api/users/me/avatar/" + minhaFoto.id())
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void perfil_do_integrante_lista_o_que_ele_publicou_no_grupo() throws Exception {
        var admin = register("Felix", "felix@ufac.br");
        var membro = register("Ana", "ana@ufac.br");
        var grupoId = createGroup(admin, "Sociedade da Roleta", "OPEN");
        addMember(grupoId, membro, "MEMBER");

        upload(membro, grupoId, "a.jpg", ExifFixtures.jpegWithoutExif(100, 100));
        upload(admin, grupoId, "b.jpg", ExifFixtures.jpegWithoutExif(100, 100));

        api(admin.accessToken())
                .get().uri("/api/users/" + membro.userId() + "/media?groupId=" + grupoId)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
    }

    private UploadPayload upload(Session user, UUID groupId, String fileName, byte[] bytes) {
        var builder = new MultipartBodyBuilder();
        builder.part("files", resource(bytes)).filename(fileName)
                .contentType(MediaType.IMAGE_JPEG);
        return send(user, groupId, builder.build());
    }

    private UploadPayload send(Session user, UUID groupId,
            MultiValueMap<String, org.springframework.http.HttpEntity<?>> body) {
        return api(user.accessToken()).post()
                .uri("/api/media?groupIds=" + groupId + "&timeZone=America/Rio_Branco")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UploadPayload.class)
                .returnResult()
                .getResponseBody();
    }

    private static ByteArrayResource resource(byte[] bytes) {
        return new ByteArrayResource(bytes);
    }

    record UploadPayload(java.util.List<MediaPayload> uploaded,
            java.util.List<RejectedPayload> rejected) {
    }

    record MediaPayload(String id, String type, String mimeType, Integer width, Integer height,
            String capturedAt, String capturedAtSource, String caption, boolean hasThumbnail) {
    }

    record RejectedPayload(String fileName, String reason) {
    }

    record UpdatePayload(String capturedAt, String caption) {
    }

    record SharePayload(java.util.List<String> groupIds) {
    }
}
