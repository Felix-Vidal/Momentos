package app.momentos.invites.dto;

import app.momentos.groups.GroupRole;
import app.momentos.invites.GroupInvite;
import java.time.Instant;
import java.util.UUID;

/**
 * Convite como a interface vê.
 *
 * @param token valor em claro, devolvido apenas na criação (depois só existe o hash)
 */
public record InviteView(
        UUID id,
        UUID groupId,
        String token,
        String invitedEmail,
        GroupRole role,
        Instant expiresAt,
        int maxUses,
        int uses,
        boolean revoked,
        Instant createdAt
) {
    public static InviteView of(GroupInvite invite, String token) {
        return new InviteView(invite.getId(), invite.getGroupId(), token,
                invite.getInvitedEmail(), invite.getRole(), invite.getExpiresAt(),
                invite.getMaxUses(), invite.getUses(), invite.getRevokedAt() != null,
                invite.getCreatedAt());
    }
}
