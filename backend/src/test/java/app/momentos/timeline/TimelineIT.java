package app.momentos.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import app.momentos.media.processing.ExifFixtures;
import app.momentos.support.ApiTest;
import app.momentos.support.IntegrationTest;
import java.time.Instant;
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
class TimelineIT extends ApiTest {

    private static final ZoneId ACRE = ZoneId.of("America/Rio_Branco");

    private Session dono;
    private UUID grupoId;

    @BeforeEach
    void cenario() {
        cleanDatabase();
        dono = register("Felix", "felix@ufac.br");
        grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");
    }

    @Test
    void midias_aparecem_em_ordem_cronologica() {
        enviarFoto("tarde.jpg", LocalDateTime.of(2026, 10, 13, 15, 0));
        enviarFoto("noite.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        enviarFoto("manha.jpg", LocalDateTime.of(2026, 10, 13, 9, 0));

        var pagina = timeline();

        assertThat(pagina.items()).hasSize(3);
        assertThat(pagina.items()).allMatch(entry -> "MEDIA".equals(entry.kind()));
        assertThat(pagina.items().stream().map(entry -> Instant.parse(entry.at())))
                .isSorted();
        assertThat(pagina.hasMore()).isFalse();
    }

    @Test
    void midias_do_intervalo_ficam_agrupadas_sob_o_titulo_do_momento() {
        enviarFoto("antes.jpg", LocalDateTime.of(2026, 10, 13, 18, 0));
        enviarFoto("corujao-1.jpg", LocalDateTime.of(2026, 10, 13, 22, 30));
        enviarFoto("corujao-2.jpg", LocalDateTime.of(2026, 10, 14, 3, 0));
        enviarFoto("depois.jpg", LocalDateTime.of(2026, 10, 14, 12, 0));

        criarMomento("Corujão da LetalCompany",
                LocalDateTime.of(2026, 10, 13, 22, 0), LocalDateTime.of(2026, 10, 14, 6, 0));

        var itens = timeline().items();

        // Duas mídias soltas e um bloco de momento com as duas da madrugada.
        assertThat(itens).hasSize(3);
        var bloco = itens.stream().filter(e -> "MOMENT".equals(e.kind())).findFirst().orElseThrow();
        assertThat(bloco.moment().moment().title()).isEqualTo("Corujão da LetalCompany");
        assertThat(bloco.moment().media()).hasSize(2);
        assertThat(bloco.moment().moment().mediaCount()).isEqualTo(2);
    }

    @Test
    void criar_momento_depois_reagrupa_as_midias_que_ja_estavam_la() {
        enviarFoto("corujao-1.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        assertThat(timeline().items()).allMatch(entry -> "MEDIA".equals(entry.kind()));

        criarMomento("Corujão da LetalCompany",
                LocalDateTime.of(2026, 10, 13, 22, 0), LocalDateTime.of(2026, 10, 14, 6, 0));

        assertThat(timeline().items()).singleElement()
                .satisfies(entry -> assertThat(entry.kind()).isEqualTo("MOMENT"));
    }

    @Test
    void apagar_o_momento_devolve_as_midias_soltas_sem_apagar_nada() {
        enviarFoto("corujao.jpg", LocalDateTime.of(2026, 10, 13, 23, 0));
        var momentoId = criarMomento("Corujão",
                LocalDateTime.of(2026, 10, 13, 22, 0), LocalDateTime.of(2026, 10, 14, 6, 0));

        api(dono.accessToken()).delete().uri("/api/moments/" + momentoId)
                .exchange().expectStatus().isNoContent();

        var itens = timeline().items();
        assertThat(itens).singleElement()
                .satisfies(entry -> assertThat(entry.kind()).isEqualTo("MEDIA"));
    }

    @Test
    void momento_sem_midias_continua_aparecendo_na_linha_do_tempo() {
        criarMomento("Churrasco que não rendeu foto",
                LocalDateTime.of(2026, 10, 20, 12, 0), LocalDateTime.of(2026, 10, 20, 18, 0));

        var itens = timeline().items();

        assertThat(itens).singleElement().satisfies(entry -> {
            assertThat(entry.kind()).isEqualTo("MOMENT");
            assertThat(entry.moment().media()).isEmpty();
        });
    }

    @Test
    void momento_mais_especifico_ganha_quando_ha_sobreposicao() {
        enviarFoto("madrugada.jpg", LocalDateTime.of(2026, 10, 13, 23, 30));
        criarMomento("Semana de provas",
                LocalDateTime.of(2026, 10, 12, 0, 0), LocalDateTime.of(2026, 10, 17, 0, 0));
        criarMomento("Corujão da LetalCompany",
                LocalDateTime.of(2026, 10, 13, 22, 0), LocalDateTime.of(2026, 10, 14, 6, 0));

        var blocos = timeline().items().stream().filter(e -> "MOMENT".equals(e.kind())).toList();

        var comMidia = blocos.stream().filter(b -> !b.moment().media().isEmpty())
                .findFirst().orElseThrow();
        assertThat(comMidia.moment().moment().title()).isEqualTo("Corujão da LetalCompany");
    }

    @Test
    void a_linha_do_tempo_e_paginada_por_cursor() {
        for (int hora = 8; hora < 14; hora++) {
            enviarFoto("foto-" + hora + ".jpg", LocalDateTime.of(2026, 10, 13, hora, 0));
        }

        var primeira = timeline(2);
        assertThat(primeira.items()).hasSize(2);
        assertThat(primeira.hasMore()).isTrue();
        assertThat(primeira.nextCursor()).isNotBlank();

        var segunda = api(dono.accessToken()).get()
                .uri("/api/groups/" + grupoId + "/timeline?limit=2&cursor=" + primeira.nextCursor())
                .exchange().expectStatus().isOk()
                .expectBody(TimelinePayload.class).returnResult().getResponseBody();

        assertThat(segunda.items()).hasSize(2);
        // A segunda página continua de onde a primeira parou, sem repetir.
        assertThat(Instant.parse(segunda.items().getFirst().at()))
                .isAfter(Instant.parse(primeira.items().getLast().at()));
    }

    @Test
    void filtro_por_dia_devolve_so_as_midias_daquele_dia() {
        enviarFoto("dia-13.jpg", LocalDateTime.of(2026, 10, 13, 15, 0));
        enviarFoto("dia-14.jpg", LocalDateTime.of(2026, 10, 14, 15, 0));

        var dia13 = api(dono.accessToken()).get()
                .uri("/api/groups/" + grupoId
                        + "/timeline/day/2026-10-13?timeZone=America/Rio_Branco")
                .exchange().expectStatus().isOk()
                .expectBody(TimelinePayload.class).returnResult().getResponseBody();

        assertThat(dia13.items()).hasSize(1);
    }

    @Test
    void o_calendario_conta_midias_por_mes_e_por_dia() {
        enviarFoto("a.jpg", LocalDateTime.of(2026, 10, 13, 15, 0));
        enviarFoto("b.jpg", LocalDateTime.of(2026, 10, 13, 16, 0));
        enviarFoto("c.jpg", LocalDateTime.of(2026, 11, 2, 10, 0));

        api(dono.accessToken()).get()
                .uri("/api/groups/" + grupoId + "/timeline/calendar?timeZone=America/Rio_Branco")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(3)
                .jsonPath("$.months.length()").isEqualTo(2)
                .jsonPath("$.months[0].month").isEqualTo("2026-10")
                .jsonPath("$.months[0].count").isEqualTo(2)
                .jsonPath("$.months[0].days[0].date").isEqualTo("2026-10-13")
                .jsonPath("$.months[1].month").isEqualTo("2026-11");
    }

    @Test
    void quem_nao_e_membro_nao_ve_a_linha_do_tempo() {
        var estranho = register("Visitante", "visita@ufac.br");
        enviarFoto("foto.jpg", LocalDateTime.of(2026, 10, 13, 15, 0));

        api(estranho.accessToken()).get().uri("/api/groups/" + grupoId + "/timeline")
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void momento_com_fim_antes_do_inicio_e_recusado() {
        api(dono.accessToken()).post().uri("/api/groups/" + grupoId + "/moments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MomentPayload("Invertido",
                        null,
                        LocalDateTime.of(2026, 10, 14, 6, 0).atZone(ACRE).toInstant().toString(),
                        LocalDateTime.of(2026, 10, 13, 22, 0).atZone(ACRE).toInstant().toString()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message")
                .value((String m) -> assertThat(m).contains("depois do início"));
    }

    @Test
    void membro_comum_pode_criar_momento_mas_nao_editar_o_de_outro() {
        var membro = register("Ana", "ana@ufac.br");
        addMember(grupoId, membro, "MEMBER");

        var meuMomento = criarMomento("Corujão",
                LocalDateTime.of(2026, 10, 13, 22, 0), LocalDateTime.of(2026, 10, 14, 6, 0));

        // Qualquer membro cria Momentos.
        api(membro.accessToken()).post().uri("/api/groups/" + grupoId + "/moments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MomentPayload("Almoço no RU", null,
                        LocalDateTime.of(2026, 10, 15, 11, 0).atZone(ACRE).toInstant().toString(),
                        LocalDateTime.of(2026, 10, 15, 14, 0).atZone(ACRE).toInstant().toString()))
                .exchange().expectStatus().isCreated();

        // Mas não mexe no momento criado por outra pessoa.
        api(membro.accessToken()).patch().uri("/api/moments/" + meuMomento)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MomentPayload("Renomeado", null, null, null))
                .exchange().expectStatus().isForbidden();

        // O administrador, sim.
        api(dono.accessToken()).patch().uri("/api/moments/" + meuMomento)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MomentPayload("Corujão da LetalCompany", null, null, null))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.title").isEqualTo("Corujão da LetalCompany");
    }

    // --- utilidades ---

    private TimelinePayload timeline() {
        return timeline(50);
    }

    private TimelinePayload timeline(int limit) {
        return api(dono.accessToken()).get()
                .uri("/api/groups/" + grupoId + "/timeline?limit=" + limit)
                .exchange().expectStatus().isOk()
                .expectBody(TimelinePayload.class).returnResult().getResponseBody();
    }

    private void enviarFoto(String nome, LocalDateTime capturadaEm) {
        try {
            var builder = new MultipartBodyBuilder();
            builder.part("files", new ByteArrayResource(
                            ExifFixtures.jpegWithCaptureDate(capturadaEm)))
                    .filename(nome).contentType(MediaType.IMAGE_JPEG);

            api(dono.accessToken()).post()
                    .uri("/api/media?groupIds=" + grupoId + "&timeZone=America/Rio_Branco")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .exchange().expectStatus().isOk();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String criarMomento(String titulo, LocalDateTime inicio, LocalDateTime fim) {
        return api(dono.accessToken()).post().uri("/api/groups/" + grupoId + "/moments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MomentPayload(titulo, null,
                        inicio.atZone(ACRE).toInstant().toString(),
                        fim.atZone(ACRE).toInstant().toString()))
                .exchange().expectStatus().isCreated()
                .expectBody(MomentResponse.class).returnResult().getResponseBody().id();
    }

    record MomentPayload(String title, String description, String startsAt, String endsAt) {
    }

    record MomentResponse(String id, String title, int mediaCount) {
    }

    record TimelinePayload(List<EntryPayload> items, String nextCursor, boolean hasMore) {
    }

    record EntryPayload(String kind, String at, BlockPayload moment, MediaPayload media) {
    }

    record BlockPayload(MomentResponse moment, List<MediaPayload> media, long commentCount) {
    }

    record MediaPayload(String id, String type, String capturedAt) {
    }
}
