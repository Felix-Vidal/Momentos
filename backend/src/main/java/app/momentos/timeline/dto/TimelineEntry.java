package app.momentos.timeline.dto;

import java.time.Instant;

/**
 * Item da linha do tempo: ou um bloco de Momento (com as mídias do intervalo),
 * ou uma mídia solta. O campo `at` é a posição cronológica usada na ordenação.
 */
public record TimelineEntry(Kind kind, Instant at, MomentBlock moment, TimelineMedia media) {

    public enum Kind {
        MOMENT,
        MEDIA
    }

    public static TimelineEntry ofMoment(MomentBlock block) {
        return new TimelineEntry(Kind.MOMENT, block.moment().startsAt(), block, null);
    }

    public static TimelineEntry ofMedia(TimelineMedia media) {
        return new TimelineEntry(Kind.MEDIA, media.media().capturedAt(), null, media);
    }
}
