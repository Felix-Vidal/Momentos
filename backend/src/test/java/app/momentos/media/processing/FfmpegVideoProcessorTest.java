package app.momentos.media.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import app.momentos.common.config.MediaProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercita o caminho com ffmpeg de verdade. Em máquina sem ffmpeg os testes são
 * ignorados — é justamente o comportamento degradado que o sistema promete.
 */
class FfmpegVideoProcessorTest {

    @TempDir
    Path temp;

    private MediaProperties properties;
    private FfmpegProbe probe;
    private FfmpegVideoProcessor processor;

    @BeforeEach
    void setUp() {
        properties = new MediaProperties(null, null, 320, "ffmpeg", true);
        probe = new FfmpegProbe(properties);
        processor = new FfmpegVideoProcessor(probe, properties);
    }

    @Test
    void extrai_miniatura_duracao_e_dimensoes_do_video() throws Exception {
        assumeTrue(probe.isAvailable(), "ffmpeg não está instalado");
        byte[] video = sampleVideo(2);

        var result = processor.process(video, ".mp4");

        assertThat(result.thumbnail()).isPresent();
        // A miniatura é um JPEG: começa com o marcador SOI.
        byte[] thumbnail = result.thumbnail().orElseThrow();
        assertThat(thumbnail[0] & 0xFF).isEqualTo(0xFF);
        assertThat(thumbnail[1] & 0xFF).isEqualTo(0xD8);

        assertThat(result.duration()).isPresent();
        assertThat(result.duration().orElseThrow().doubleValue()).isBetween(1.5, 2.5);
        assertThat(result.width()).contains(320);
        assertThat(result.height()).contains(240);
    }

    @Test
    void sem_ffmpeg_o_resultado_e_vazio_e_o_video_original_e_preservado() {
        var semFfmpeg = new FfmpegVideoProcessor(
                new FfmpegProbe(new MediaProperties(null, null, 320, "ffmpeg-que-nao-existe", true)),
                properties);

        var result = semFfmpeg.process(new byte[] {1, 2, 3}, ".mp4");

        assertThat(result.compressed()).isEmpty();
        assertThat(result.thumbnail()).isEmpty();
        assertThat(result.duration()).isEmpty();
    }

    @Test
    void compressao_desligada_mantem_o_arquivo_enviado() throws Exception {
        assumeTrue(probe.isAvailable(), "ffmpeg não está instalado");
        var semCompressao = new FfmpegVideoProcessor(probe,
                new MediaProperties(null, null, 320, "ffmpeg", false));

        var result = semCompressao.process(sampleVideo(1), ".mp4");

        assertThat(result.compressed()).isEmpty();
        // A miniatura continua sendo gerada: o que foi desligado é só a recompressão.
        assertThat(result.thumbnail()).isPresent();
    }

    @Test
    void arquivo_que_nao_e_video_nao_quebra_o_processamento() {
        assumeTrue(probe.isAvailable(), "ffmpeg não está instalado");

        var result = processor.process("isto nao e um video".getBytes(), ".mp4");

        assertThat(result.thumbnail()).isEmpty();
        assertThat(result.compressed()).isEmpty();
    }

    /** Gera um vídeo de teste com o próprio ffmpeg (padrão colorido, 320x240). */
    private byte[] sampleVideo(int seconds) throws IOException, InterruptedException {
        Path output = temp.resolve("amostra.mp4");
        Process process = new ProcessBuilder(List.of("ffmpeg", "-y",
                "-f", "lavfi", "-i", "testsrc=size=320x240:rate=15:duration=" + seconds,
                "-c:v", "libx264", "-pix_fmt", "yuv420p", output.toString()))
                .redirectErrorStream(true)
                .start();
        byte[] ignored = process.getInputStream().readAllBytes();
        assumeTrue(process.waitFor(2, TimeUnit.MINUTES) && process.exitValue() == 0,
                "ffmpeg não conseguiu gerar o vídeo de teste");
        return Files.readAllBytes(output);
    }
}
