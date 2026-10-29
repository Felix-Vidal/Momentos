package app.momentos.security;

import java.util.UUID;

/**
 * Usuário autenticado na requisição. Chega aos controllers por
 * {@code @AuthenticationPrincipal CurrentUser me}.
 */
public record CurrentUser(UUID id, String email, String name) {
}
