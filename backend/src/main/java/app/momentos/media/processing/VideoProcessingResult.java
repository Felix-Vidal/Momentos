package app.momentos.media.processing;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Resultado do processamento de um vídeo.
 *
 * @param compressed vídeo recomprimido, quando houve compressão
 * @param thumbnail  quadro extraído como miniatura JPEG
 * @param duration   duração em segundos
 * @param width      largura em pixels
 * @param height     altura em pixels
 */
public record VideoProcessingResult(
        Optional<byte[]> compressed,
        Optional<byte[]> thumbnail,
        Optional<BigDecimal> duration,
        Optional<Integer> width,
        Optional<Integer> height
) {
    public static VideoProcessingResult empty() {
        return new VideoProcessingResult(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }
}
