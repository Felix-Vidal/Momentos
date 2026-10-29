package app.momentos.invites.dto;

import java.util.UUID;

/**
 * O que um convite revela a quem ainda não é membro: apenas a identificação do grupo,
 * para a pessoa saber onde está entrando.
 */
public record InvitePreview(
        UUID groupId,
        String groupName,
        String groupDescription,
        UUID coverMediaId,
        String invitedBy,
        boolean valid,
        boolean alreadyMember,
        String reason
) {
}
