package app.momentos.moments.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateMomentRequest(
        @Size(min = 2, max = 120, message = "O título deve ter de 2 a 120 caracteres.")
        String title,

        @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
        String description,

        Instant startsAt,
        Instant endsAt
) {
}
