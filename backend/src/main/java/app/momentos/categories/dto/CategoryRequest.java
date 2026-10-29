package app.momentos.categories.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Dê um nome à categoria.")
        @Size(min = 2, max = 40, message = "O nome deve ter de 2 a 40 caracteres.")
        String name,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Escolha uma cor válida.")
        String color,

        @Size(max = 16, message = "Use apenas um emoji.")
        String emoji
) {
}
