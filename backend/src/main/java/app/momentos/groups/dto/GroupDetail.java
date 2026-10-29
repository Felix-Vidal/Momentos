package app.momentos.groups.dto;

import app.momentos.groups.Group;
import app.momentos.groups.GroupRole;
import app.momentos.groups.GroupTheme;
import app.momentos.groups.GroupVisibility;
import java.time.Instant;
import java.util.UUID;

public record GroupDetail(
        UUID id,
        String slug,
        String name,
        String description,
        UUID coverMediaId,
        GroupVisibility visibility,
        GroupTheme theme,
        GroupRole myRole,
        long memberCount,
        Instant createdAt
) {
    public static GroupDetail of(Group group, GroupRole myRole, long memberCount) {
        return new GroupDetail(group.getId(), group.getSlug(), group.getName(),
                group.getDescription(), group.getCoverMediaId(), group.getVisibility(),
                group.getTheme(), myRole, memberCount, group.getCreatedAt());
    }
}
