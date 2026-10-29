package app.momentos.users.dto;

import app.momentos.users.User;
import java.util.UUID;

/** Identificação mínima de um usuário, usada em listas, autoria e comentários. */
public record UserSummary(UUID id, String name, UUID avatarMediaId) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getName(), user.getAvatarMediaId());
    }
}
