package app.momentos.groups.dto;

import app.momentos.groups.GroupTheme;
import app.momentos.groups.GroupVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank(message = "Dê um nome ao grupo.")
        @Size(min = 2, max = 80, message = "O nome deve ter de 2 a 80 caracteres.")
        String name,

        @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
        String description,

        @NotNull(message = "Escolha se o grupo é aberto ou privado.")
        GroupVisibility visibility,

        GroupTheme theme
) {
}
