package app.momentos.comments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Escreva algo antes de enviar.")
        @Size(max = 1000, message = "O comentário pode ter no máximo 1000 caracteres.")
        String body
) {
}
