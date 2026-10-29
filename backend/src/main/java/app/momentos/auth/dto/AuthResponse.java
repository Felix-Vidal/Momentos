package app.momentos.auth.dto;

import app.momentos.users.dto.UserProfile;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserProfile user
) {
}
