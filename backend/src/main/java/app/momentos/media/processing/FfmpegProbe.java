package app.momentos.media.processing;

import app.momentos.common.config.MediaProperties;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Descobre, na subida, se o ffmpeg está disponível.
 *
 * <p>Sem ffmpeg o sistema continua funcionando: o vídeo é guardado como veio e o
 * player usa um poster padrão. Com ffmpeg, o vídeo é comprimido e ganha miniatura.
 */
@Component
public class FfmpegProbe {

    private static final Logger log = LoggerFactory.getLogger(FfmpegProbe.class);

    private final boolean available;
    private final String ffmpeg;

    public FfmpegProbe(MediaProperties properties) {
        this.ffmpeg = properties.ffmpegPath();
        this.available = probe(ffmpeg);
        if (available) {
            log.info("ffmpeg encontrado ({}): vídeos serão comprimidos e ganharão miniatura.",
                    ffmpeg);
        } else {
            log.info("ffmpeg não encontrado: vídeos serão guardados como enviados, "
                    + "sem compressão nem miniatura.");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public String command() {
        return ffmpeg;
    }

    private static boolean probe(String command) {
        try {
            Process process = new ProcessBuilder(command, "-version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
