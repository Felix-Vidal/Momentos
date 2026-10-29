package app.momentos.media.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ExifCaptureDateExtractorTest {

    private static final ZoneId ACRE = ZoneId.of("America/Rio_Branco");  // UTC-5

    private final ExifCaptureDateExtractor extractor = new ExifCaptureDateExtractor();

    @Test
    void le_a_data_de_captura_do_exif_no_fuso_informado() throws Exception {
        // A foto do Corujão: 13/10 às 22:30 no horário do Acre.
        byte[] jpeg = ExifFixtures.jpegWithCaptureDate(LocalDateTime.of(2026, 10, 13, 22, 30, 0));

        var capturedAt = extractor.extract(new ByteArrayInputStream(jpeg), ACRE);

        assertThat(capturedAt).isPresent();
        assertThat(capturedAt.get())
                .isEqualTo(LocalDateTime.of(2026, 10, 13, 22, 30, 0)
                        .atZone(ACRE).toInstant());
    }

    @Test
    void o_mesmo_exif_em_outro_fuso_resulta_em_instante_diferente() throws Exception {
        byte[] jpeg = ExifFixtures.jpegWithCaptureDate(LocalDateTime.of(2026, 10, 13, 22, 30, 0));

        var noAcre = extractor.extract(new ByteArrayInputStream(jpeg), ACRE);
        var emUtc = extractor.extract(new ByteArrayInputStream(jpeg), ZoneOffset.UTC);

        // O EXIF guarda hora local sem fuso: 22:30 no Acre é 03:30 UTC do dia seguinte.
        assertThat(noAcre).isPresent();
        assertThat(emUtc).isPresent();
        assertThat(java.time.Duration.between(emUtc.get(), noAcre.get()).toHours()).isEqualTo(5);
    }

    @Test
    void imagem_sem_exif_nao_tem_data_de_captura() throws Exception {
        byte[] jpeg = ExifFixtures.jpegWithoutExif(120, 80);

        assertThat(extractor.extract(new ByteArrayInputStream(jpeg), ACRE)).isEmpty();
    }

    @Test
    void arquivo_que_nao_e_imagem_nao_quebra_a_leitura() {
        byte[] lixo = "isto nao e uma imagem".getBytes();

        assertThat(extractor.extract(new ByteArrayInputStream(lixo), ACRE)).isEmpty();
    }
}
