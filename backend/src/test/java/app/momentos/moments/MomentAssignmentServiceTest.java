package app.momentos.moments;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A regra de quais mídias caem em cada Momento é o coração da linha do tempo,
 * por isso está isolada e coberta caso a caso.
 */
class MomentAssignmentServiceTest {

    private static final ZoneId ACRE = ZoneId.of("America/Rio_Branco");
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();

    private final MomentAssignmentService service = new MomentAssignmentService();

    @Test
    void midia_dentro_do_intervalo_pertence_ao_momento() {
        // Corujão da LetalCompany: 13/10 22:00 → 14/10 06:00, atravessando a meia-noite.
        Moment corujao = moment("Corujão da LetalCompany",
                at(2026, 10, 13, 22, 0), at(2026, 10, 14, 6, 0));

        assertThat(service.assign(at(2026, 10, 13, 23, 45), List.of(corujao)))
                .contains(corujao);
        assertThat(service.assign(at(2026, 10, 14, 3, 12), List.of(corujao)))
                .contains(corujao);
    }

    @Test
    void o_inicio_pertence_ao_momento_e_o_fim_nao() {
        Moment corujao = moment("Corujão", at(2026, 10, 13, 22, 0), at(2026, 10, 14, 6, 0));

        // Intervalo meio-aberto: evita que duas faixas vizinhas disputem a mesma mídia.
        assertThat(service.assign(at(2026, 10, 13, 22, 0), List.of(corujao))).contains(corujao);
        assertThat(service.assign(at(2026, 10, 14, 6, 0), List.of(corujao))).isEmpty();
    }

    @Test
    void midia_fora_do_intervalo_fica_solta_na_linha_do_tempo() {
        Moment corujao = moment("Corujão", at(2026, 10, 13, 22, 0), at(2026, 10, 14, 6, 0));

        assertThat(service.assign(at(2026, 10, 13, 21, 59), List.of(corujao))).isEmpty();
        assertThat(service.assign(at(2026, 10, 15, 12, 0), List.of(corujao))).isEmpty();
    }

    @Test
    void momento_de_dia_inteiro_cobre_o_dia_todo() {
        Moment dia = moment("Almoço no RU", at(2026, 10, 13, 0, 0), at(2026, 10, 14, 0, 0));

        assertThat(service.assign(at(2026, 10, 13, 0, 0), List.of(dia))).contains(dia);
        assertThat(service.assign(at(2026, 10, 13, 12, 30), List.of(dia))).contains(dia);
        assertThat(service.assign(at(2026, 10, 13, 23, 59), List.of(dia))).contains(dia);
        assertThat(service.assign(at(2026, 10, 14, 0, 0), List.of(dia))).isEmpty();
    }

    @Test
    void momento_que_atravessa_vários_dias_tambem_vale() {
        Moment viagem = moment("Viagem a Porto Velho",
                at(2026, 10, 10, 6, 0), at(2026, 10, 13, 20, 0));

        assertThat(service.assign(at(2026, 10, 11, 15, 0), List.of(viagem))).contains(viagem);
        assertThat(service.assign(at(2026, 10, 12, 2, 0), List.of(viagem))).contains(viagem);
    }

    @Test
    void entre_momentos_sobrepostos_vale_o_mais_especifico() {
        Moment viagem = moment("Viagem a Porto Velho",
                at(2026, 10, 13, 6, 0), at(2026, 10, 16, 6, 0));
        Moment corujao = moment("Corujão da LetalCompany",
                at(2026, 10, 13, 22, 0), at(2026, 10, 14, 6, 0));

        // Dentro da viagem, mas também dentro do corujão: o recorte menor ganha.
        assertThat(service.assign(at(2026, 10, 13, 23, 0), List.of(viagem, corujao)))
                .contains(corujao);
        // Fora do corujão, sobra a viagem.
        assertThat(service.assign(at(2026, 10, 14, 10, 0), List.of(viagem, corujao)))
                .contains(viagem);
    }

    @Test
    void a_ordem_da_lista_nao_muda_o_resultado() {
        Moment viagem = moment("Viagem", at(2026, 10, 13, 6, 0), at(2026, 10, 16, 6, 0));
        Moment corujao = moment("Corujão", at(2026, 10, 13, 22, 0), at(2026, 10, 14, 6, 0));

        assertThat(service.assign(at(2026, 10, 13, 23, 0), List.of(corujao, viagem)))
                .contains(corujao);
    }

    @Test
    void momentos_de_mesma_duracao_desempatam_pelo_criado_mais_recente() {
        Moment antigo = moment("Primeira tentativa",
                at(2026, 10, 13, 20, 0), at(2026, 10, 14, 2, 0));
        Moment recente = moment("Nome definitivo",
                at(2026, 10, 13, 20, 0), at(2026, 10, 14, 2, 0));
        // `recente` foi criado depois, então reflete a intenção mais atual.
        setCreatedAt(antigo, Instant.parse("2026-10-20T10:00:00Z"));
        setCreatedAt(recente, Instant.parse("2026-10-21T10:00:00Z"));

        assertThat(service.assign(at(2026, 10, 13, 23, 0), List.of(antigo, recente)))
                .contains(recente);
    }

    @Test
    void sem_momentos_a_midia_fica_solta() {
        assertThat(service.assign(at(2026, 10, 13, 23, 0), List.of())).isEmpty();
        assertThat(service.assign(at(2026, 10, 13, 23, 0), null)).isEmpty();
    }

    @Test
    void momento_de_um_segundo_e_valido() {
        Moment instante = moment("A roleta girou",
                at(2026, 10, 13, 12, 0), at(2026, 10, 13, 12, 0).plusSeconds(1));

        assertThat(service.assign(at(2026, 10, 13, 12, 0), List.of(instante))).contains(instante);
        assertThat(service.assign(at(2026, 10, 13, 12, 0).plusSeconds(1), List.of(instante)))
                .isEmpty();
    }

    private static Moment moment(String title, Instant startsAt, Instant endsAt) {
        return new Moment(GRUPO, title, null, startsAt, endsAt, AUTOR);
    }

    private static Instant at(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(ACRE).toInstant();
    }

    /** O createdAt é atribuído na construção; o desempate precisa controlá-lo. */
    private static void setCreatedAt(Moment moment, Instant value) {
        try {
            var field = Moment.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(moment, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
