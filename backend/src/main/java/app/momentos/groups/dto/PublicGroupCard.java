package app.momentos.groups.dto;

import app.momentos.groups.Group;
import java.util.UUID;

/**
 * O que um grupo aberto mostra a quem ainda não é membro: apenas a capa do grupo,
 * nunca o conteúdo.
 */
public record PublicGroupCard(
        UUID id,
        String slug,
        String name,
        String description,
        UUID coverMediaId,
        long memberCount,
        boolean member,
        boolean requested
) {
    public static PublicGroupCard of(Group group, long memberCount, boolean member,
            boolean requested) {
        return new PublicGroupCard(group.getId(), group.getSlug(), group.getName(),
                group.getDescription(), group.getCoverMediaId(), memberCount, member, requested);
    }
}
