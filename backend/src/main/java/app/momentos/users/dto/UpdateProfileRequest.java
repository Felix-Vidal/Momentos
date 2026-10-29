package app.momentos.users.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 80, message = "O nome deve ter de 2 a 80 caracteres.")
        String name,

        @Size(max = 280, message = "A bio pode ter no máximo 280 caracteres.")
        String bio
) {
}
