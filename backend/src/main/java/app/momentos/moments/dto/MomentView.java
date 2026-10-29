package app.momentos.moments.dto;

import app.momentos.moments.Moment;
import app.momentos.users.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record MomentView(
        UUID id,
        UUID groupId,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        int mediaCount,
        UserSummary createdBy,
        Instant createdAt
) {
    public static MomentView of(Moment moment, int mediaCount, UserSummary createdBy) {
        return new MomentView(moment.getId(), moment.getGroupId(), moment.getTitle(),
                moment.getDescription(), moment.getStartsAt(), moment.getEndsAt(),
                mediaCount, createdBy, moment.getCreatedAt());
    }
}
