package app.momentos.groups.dto;

import app.momentos.groups.GroupMember;
import app.momentos.groups.GroupRole;
import app.momentos.users.User;
import java.time.Instant;
import java.util.UUID;

public record GroupMemberView(
        UUID userId,
        String name,
        String bio,
        UUID avatarMediaId,
        GroupRole role,
        Instant joinedAt
) {
    public static GroupMemberView of(GroupMember member, User user) {
        return new GroupMemberView(user.getId(), user.getName(), user.getBio(),
                user.getAvatarMediaId(), member.getRole(), member.getJoinedAt());
    }
}
