package app.momentos.invites.dto;

import app.momentos.invites.JoinRequest;
import java.time.Instant;
import java.util.UUID;

public record JoinRequestView(
        UUID id,
        UUID groupId,
        UUID userId,
        String userName,
        UUID userAvatarMediaId,
        String message,
        Instant createdAt
) {
    public static JoinRequestView of(JoinRequest request, String userName, UUID avatarMediaId) {
        return new JoinRequestView(request.getId(), request.getGroupId(), request.getUserId(),
                userName, avatarMediaId, request.getMessage(), request.getCreatedAt());
    }
}
