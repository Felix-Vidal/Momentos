package app.momentos.users.dto;

import app.momentos.users.User;
import java.time.Instant;
import java.util.UUID;

public record UserProfile(
        UUID id,
        String name,
        String email,
        String bio,
        UUID avatarMediaId,
        Instant createdAt
) {
    /** Perfil completo, só para o próprio usuário (inclui e-mail). */
    public static UserProfile own(User user) {
        return new UserProfile(
                user.getId(), user.getName(), user.getEmail(), user.getBio(),
                user.getAvatarMediaId(), user.getCreatedAt());
    }

    /** Perfil visto por outras pessoas: sem e-mail. */
    public static UserProfile publicView(User user) {
        return new UserProfile(
                user.getId(), user.getName(), null, user.getBio(),
                user.getAvatarMediaId(), user.getCreatedAt());
    }
}
