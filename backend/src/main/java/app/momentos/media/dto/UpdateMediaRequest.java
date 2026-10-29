package app.momentos.media.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Ajustes do dono da mídia.
 *
 * @param capturedAt corrige a data/hora de captura (quando o EXIF faltou ou veio errado)
 */
public record UpdateMediaRequest(
        Instant capturedAt,
        @Size(max = 280, message = "A legenda pode ter no máximo 280 caracteres.") String caption
) {
}
