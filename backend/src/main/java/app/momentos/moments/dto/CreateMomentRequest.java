package app.momentos.moments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateMomentRequest(
        @NotBlank(message = "Dê um título ao momento.")
        @Size(min = 2, max = 120, message = "O título deve ter de 2 a 120 caracteres.")
        String title,

        @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
        String description,

        @NotNull(message = "Informe quando o momento começa.")
        Instant startsAt,

        @NotNull(message = "Informe quando o momento termina.")
        Instant endsAt
) {
}
