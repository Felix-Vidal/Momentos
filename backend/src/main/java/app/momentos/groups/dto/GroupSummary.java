package app.momentos.groups.dto;

import app.momentos.groups.Group;
import app.momentos.groups.GroupRole;
import app.momentos.groups.GroupTheme;
import app.momentos.groups.GroupVisibility;
import java.util.UUID;

/** Cartão de grupo nas listagens de quem já é membro. */
public record GroupSummary(
        UUID id,
        String slug,
        String name,
        String description,
        UUID coverMediaId,
        GroupVisibility visibility,
        GroupTheme theme,
        GroupRole myRole,
        long memberCount
) {
    public static GroupSummary of(Group group, GroupRole myRole, long memberCount) {
        return new GroupSummary(group.getId(), group.getSlug(), group.getName(),
                group.getDescription(), group.getCoverMediaId(), group.getVisibility(),
                group.getTheme(), myRole, memberCount);
    }
}
