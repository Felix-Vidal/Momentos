package app.momentos.groups;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SlugsTest {

    @Test
    void remove_acentos_e_pontuacao() {
        assertThat(Slugs.from("Sociedade da Roleta")).isEqualTo("sociedade-da-roleta");
        assertThat(Slugs.from("Corujão da LetalCompany!")).isEqualTo("corujao-da-letalcompany");
        assertThat(Slugs.from("  Café   com  Ação  ")).isEqualTo("cafe-com-acao");
    }

    @Test
    void nome_sem_letras_tem_slug_padrao() {
        assertThat(Slugs.from("??? !!!")).isEqualTo("grupo");
    }

    @Test
    void slug_longo_e_truncado_sem_terminar_em_hifen() {
        String slug = Slugs.from("a".repeat(40) + " " + "b".repeat(40));
        assertThat(slug).hasSizeLessThanOrEqualTo(60).doesNotEndWith("-");
    }

    @Test
    void slug_repetido_recebe_sufixo_numerico() {
        Set<String> existentes = Set.of("sociedade-da-roleta", "sociedade-da-roleta-2");

        assertThat(Slugs.unique("Sociedade da Roleta", existentes::contains))
                .isEqualTo("sociedade-da-roleta-3");
    }
}
