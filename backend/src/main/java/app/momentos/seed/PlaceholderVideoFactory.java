package app.momentos.seed;

import app.momentos.media.processing.FfmpegProbe;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gera os vídeos do seed a partir de um quadro desenhado em Java, animado com um
 * leve zoom pelo ffmpeg.
 *
 * <p>Sem ffmpeg na máquina, devolve vazio e o seed simplesmente não cria vídeos —
 * a mesma degradação que o upload de verdade tem.
 */
@Component
public class PlaceholderVideoFactory {

    private static final Logger log = LoggerFactory.getLogger(PlaceholderVideoFactory.class);

    private final FfmpegProbe probe;
    private final PlaceholderImageFactory images;

    public PlaceholderVideoFactory(FfmpegProbe probe, PlaceholderImageFactory images) {
        this.probe = probe;
        this.images = images;
    }

    public Optional<byte[]> create(String label, Instant capturedAt, ZoneId zone, int seconds) {
        if (!probe.isAvailable()) {
            return Optional.empty();
        }

        Path work = null;
        try {
            work = Files.createTempDirectory("momentos-seed-video");
            Path frame = work.resolve("quadro.jpg");
            Path output = work.resolve("video.mp4");
            Files.write(frame, images.create(label, capturedAt, zone, 1280, 720));

            Process process = new ProcessBuilder(List.of(probe.command(), "-y",
                    "-loop", "1", "-i", frame.toString(),
                    "-t", String.valueOf(seconds),
                    // Zoom lento para o vídeo ter movimento sem precisar de fonte no ffmpeg.
                    "-vf", "zoompan=z='min(zoom+0.0012,1.25)':d=" + (seconds * 25)
                            + ":s=1280x720:fps=25,format=yuv420p",
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "28",
                    "-movflags", "+faststart", output.toString()))
                    .redirectErrorStream(true)
                    .start();
            byte[] ignored = process.getInputStream().readAllBytes();

            if (!process.waitFor(3, TimeUnit.MINUTES) || process.exitValue() != 0
                    || !Files.exists(output)) {
                log.warn("Não foi possível gerar o vídeo de demonstração '{}'.", label);
                return Optional.empty();
            }
            return Optional.of(Files.readAllBytes(output));
        } catch (IOException ex) {
            log.warn("Falha ao gerar vídeo de demonstração: {}", ex.getMessage());
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            deleteRecursively(work);
        }
    }

    private static void deleteRecursively(Path directory) {
        if (directory == null) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Arquivo temporário remanescente não impede o seed.
                }
            });
        } catch (IOException ignored) {
            // idem
        }
    }
}
