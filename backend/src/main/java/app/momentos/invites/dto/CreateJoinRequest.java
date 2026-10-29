package app.momentos.invites.dto;

import jakarta.validation.constraints.Size;

public record CreateJoinRequest(
        @Size(max = 280, message = "A mensagem pode ter no máximo 280 caracteres.")
        String message
) {
}
