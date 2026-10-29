package app.momentos.groups.dto;

import app.momentos.groups.GroupTheme;
import app.momentos.groups.GroupVisibility;
import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(
        @Size(min = 2, max = 80, message = "O nome deve ter de 2 a 80 caracteres.")
        String name,

        @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
        String description,

        GroupVisibility visibility,
        GroupTheme theme
) {
}
