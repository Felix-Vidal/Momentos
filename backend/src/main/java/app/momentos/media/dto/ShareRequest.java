package app.momentos.media.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ShareRequest(
        @NotEmpty(message = "Escolha pelo menos um grupo.") List<UUID> groupIds
) {
}
