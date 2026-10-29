package app.momentos.groups.dto;

import app.momentos.groups.GroupRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull(message = "Informe o papel.") GroupRole role) {
}
