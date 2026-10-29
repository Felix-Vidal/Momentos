package app.momentos.media.dto;

import app.momentos.media.CapturedAtSource;
import app.momentos.media.Media;
import app.momentos.media.MediaType;
import app.momentos.users.dto.UserSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Mídia como a interface a consome. */
public record MediaView(
        UUID id,
        MediaType type,
        String mimeType,
        Integer width,
        Integer height,
        BigDecimal durationSeconds,
        Instant capturedAt,
        CapturedAtSource capturedAtSource,
        String caption,
        boolean hasThumbnail,
        UserSummary owner
) {
    public static MediaView of(Media media, UserSummary owner) {
        return new MediaView(media.getId(), media.getType(), media.getMimeType(),
                media.getWidth(), media.getHeight(), media.getDurationSeconds(),
                media.getCapturedAt(), media.getCapturedAtSource(), media.getCaption(),
                media.getThumbnailKey() != null, owner);
    }
}
