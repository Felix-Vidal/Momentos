package app.momentos.media.processing;

import app.momentos.common.config.MediaProperties;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Comprime vídeos e extrai a miniatura usando o ffmpeg, quando ele existe.
 *
 * <p>Qualquer falha aqui é tratada como "sem processamento": o vídeo original é
 * preservado, porque perder a mídia do usuário seria pior que não ter miniatura.
 */
@Component
public class FfmpegVideoProcessor {

    private static final Logger log = LoggerFactory.getLogger(FfmpegVideoProcessor.class);
    private static final long TIMEOUT_MINUTES = 10;

    private final FfmpegProbe probe;
    private final MediaProperties properties;

    public FfmpegVideoProcessor(FfmpegProbe probe, MediaProperties properties) {
        this.probe = probe;
        this.properties = properties;
    }

    public VideoProcessingResult process(byte[] original, String extension) {
        if (!probe.isAvailable()) {
            return VideoProcessingResult.empty();
        }

        Path work = null;
        try {
            work = Files.createTempDirectory("momentos-video");
            Path input = work.resolve("entrada" + (extension.isBlank() ? ".mp4" : extension));
            Files.write(input, original);

            Optional<byte[]> thumbnail = extractThumbnail(work, input);
            Optional<byte[]> compressed = properties.compressVideo()
                    ? compress(work, input, original.length)
                    : Optional.empty();
            var dimensions = readDimensions(input);

            return new VideoProcessingResult(compressed, thumbnail, readDuration(input),
                    dimensions.map(d -> d[0]), dimensions.map(d -> d[1]));
        } catch (IOException ex) {
            log.warn("Falha ao processar vídeo: {}", ex.getMessage());
            return VideoProcessingResult.empty();
        } finally {
            deleteRecursively(work);
        }
    }

    private Optional<byte[]> extractThumbnail(Path work, Path input) throws IOException {
        Path output = work.resolve("miniatura.jpg");
        boolean ok = run(List.of(probe.command(), "-y", "-i", input.toString(),
                // Primeiro quadro representativo, redimensionado para a largura padrão.
                "-vf", "thumbnail,scale=" + properties.thumbnailWidth() + ":-2",
                "-frames:v", "1", output.toString()));
        return ok && Files.exists(output) ? Optional.of(Files.readAllBytes(output)) : Optional.empty();
    }

    private Optional<byte[]> compress(Path work, Path input, long originalSize) throws IOException {
        Path output = work.resolve("comprimido.mp4");
        boolean ok = run(List.of(probe.command(), "-y", "-i", input.toString(),
                "-c:v", "libx264", "-preset", "medium", "-crf", "26",
                // Garante dimensões pares, exigidas pelo H.264.
                "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2",
                "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart",
                output.toString()));
        if (!ok || !Files.exists(output)) {
            return Optional.empty();
        }
        byte[] compressed = Files.readAllBytes(output);
        // Só vale trocar se o resultado ficou menor que o original.
        return compressed.length < originalSize ? Optional.of(compressed) : Optional.empty();
    }

    private Optional<BigDecimal> readDuration(Path input) {
        return readMetadata(input, "duration").map(value -> {
            try {
                return new BigDecimal(value).setScale(3, java.math.RoundingMode.HALF_UP);
            } catch (NumberFormatException ex) {
                return null;
            }
        });
    }

    private Optional<int[]> readDimensions(Path input) {
        Optional<String> width = readMetadata(input, "width");
        Optional<String> height = readMetadata(input, "height");
        if (width.isEmpty() || height.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new int[] {
                    Integer.parseInt(width.get().trim()), Integer.parseInt(height.get().trim())});
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /** Usa o ffprobe que acompanha o ffmpeg para ler uma propriedade do vídeo. */
    private Optional<String> readMetadata(Path input, String property) {
        String ffprobe = probe.command().replaceAll("ffmpeg$", "ffprobe");
        String entry = "duration".equals(property) ? "format=duration" : "stream=" + property;
        try {
            Process process = new ProcessBuilder(ffprobe, "-v", "error",
                    "-select_streams", "v:0", "-show_entries", entry,
                    "-of", "default=noprint_wrappers=1:nokey=1", input.toString())
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0
                    || output.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(output.lines().findFirst().orElse("").trim());
        } catch (IOException ex) {
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private boolean run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            // Descarta a saída para o processo não travar enchendo o buffer do pipe.
            byte[] ignored = process.getInputStream().readAllBytes();
            if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                log.warn("ffmpeg excedeu {} minutos e foi interrompido.", TIMEOUT_MINUTES);
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException ex) {
            log.warn("ffmpeg falhou: {}", ex.getMessage());
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
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
                    // Arquivo temporário remanescente não impede o fluxo.
                }
            });
        } catch (IOException ignored) {
            // idem
        }
    }
}
