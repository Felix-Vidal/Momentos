package app.momentos.timeline.dto;

import app.momentos.media.dto.MediaView;
import java.time.Instant;

/**
 * Item da linha do tempo: ou um bloco de Momento (com as mídias do intervalo),
 * ou uma mídia solta. O campo `at` é a posição cronológica usada na ordenação.
 */
public record TimelineEntry(Kind kind, Instant at, MomentBlock moment, MediaView media) {

    public enum Kind {
        MOMENT,
        MEDIA
    }

    public static TimelineEntry ofMoment(MomentBlock block) {
        return new TimelineEntry(Kind.MOMENT, block.moment().startsAt(), block, null);
    }

    public static TimelineEntry ofMedia(MediaView media) {
        return new TimelineEntry(Kind.MEDIA, media.capturedAt(), null, media);
    }
}
