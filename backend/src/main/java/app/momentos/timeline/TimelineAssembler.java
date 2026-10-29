package app.momentos.timeline;

import app.momentos.media.Media;
import app.momentos.media.dto.MediaView;
import app.momentos.moments.Moment;
import app.momentos.moments.MomentAssignmentService;
import app.momentos.moments.dto.MomentView;
import app.momentos.timeline.dto.MomentBlock;
import app.momentos.timeline.dto.TimelineEntry;
import app.momentos.users.dto.UserSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Transforma uma página de mídias em entradas da linha do tempo, agrupando sob os
 * Momentos a que pertencem.
 *
 * <p>Momentos sem nenhuma mídia continuam aparecendo: o título já conta parte da
 * história e aceita comentários.
 */
@Component
public class TimelineAssembler {

    private final MomentAssignmentService assignment;

    public TimelineAssembler(MomentAssignmentService assignment) {
        this.assignment = assignment;
    }

    /**
     * @param emptyMomentsWindowEnd até onde incluir Momentos vazios nesta página
     *                              (normalmente o início da página seguinte)
     */
    public List<TimelineEntry> assemble(
            List<Media> page,
            List<Moment> moments,
            Map<UUID, UserSummary> owners,
            Function<Moment, Long> commentCountOf,
            Instant windowStart,
            Instant emptyMomentsWindowEnd) {

        Map<UUID, List<MediaView>> byMoment = new LinkedHashMap<>();
        Map<UUID, Moment> momentsById = new LinkedHashMap<>();
        List<TimelineEntry> loose = new ArrayList<>();

        for (Media media : page) {
            MediaView view = MediaView.of(media, owners.get(media.getOwnerId()));
            var owner = assignment.assign(media.getCapturedAt(), moments);
            if (owner.isPresent()) {
                Moment moment = owner.get();
                momentsById.putIfAbsent(moment.getId(), moment);
                byMoment.computeIfAbsent(moment.getId(), key -> new ArrayList<>()).add(view);
            } else {
                loose.add(TimelineEntry.ofMedia(view));
            }
        }

        List<TimelineEntry> entries = new ArrayList<>(loose);

        byMoment.forEach((momentId, mediaList) -> {
            Moment moment = momentsById.get(momentId);
            entries.add(TimelineEntry.ofMoment(new MomentBlock(
                    MomentView.of(moment, mediaList.size(), owners.get(moment.getCreatedBy())),
                    mediaList,
                    commentCountOf.apply(moment),
                    List.of())));
        });

        // Momentos sem mídia na janela desta página entram vazios, para o título existir.
        moments.stream()
                .filter(moment -> !byMoment.containsKey(moment.getId()))
                .filter(moment -> !moment.getStartsAt().isBefore(windowStart))
                .filter(moment -> moment.getStartsAt().isBefore(emptyMomentsWindowEnd))
                .forEach(moment -> entries.add(TimelineEntry.ofMoment(new MomentBlock(
                        MomentView.of(moment, 0, owners.get(moment.getCreatedBy())),
                        List.of(),
                        commentCountOf.apply(moment),
                        List.of()))));

        entries.sort(Comparator.comparing(TimelineEntry::at));
        return entries;
    }
}
