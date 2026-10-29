package app.momentos.timeline;

import app.momentos.categories.CategoryService;
import app.momentos.categories.dto.CategoryView;
import app.momentos.comments.dto.CommentView;
import app.momentos.groups.GroupRole;
import app.momentos.media.Media;
import app.momentos.media.dto.MediaView;
import app.momentos.moments.Moment;
import app.momentos.moments.MomentAssignmentService;
import app.momentos.moments.dto.MomentView;
import app.momentos.timeline.dto.MomentBlock;
import app.momentos.timeline.dto.TimelineEntry;
import app.momentos.timeline.dto.TimelineMedia;
import app.momentos.users.dto.UserSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Transforma uma página de mídias em entradas da linha do tempo, agrupando sob os
 * Momentos a que pertencem e anexando a conversa de cada item.
 *
 * <p>Momentos sem nenhuma mídia continuam aparecendo: o título já conta parte da
 * história e aceita comentários.
 */
@Component
public class TimelineAssembler {

    /** Quantos comentários acompanham cada mídia e cada bloco de Momento. */
    private static final int COMMENTS_PER_MEDIA = 8;
    private static final int COMMENTS_PER_MOMENT = 16;

    private final MomentAssignmentService assignment;
    private final CategoryService categories;

    public TimelineAssembler(MomentAssignmentService assignment, CategoryService categories) {
        this.assignment = assignment;
        this.categories = categories;
    }

    /**
     * @param emptyMomentsWindowEnd até onde incluir Momentos vazios nesta página
     *                              (normalmente o início da página seguinte)
     */
    public List<TimelineEntry> assemble(
            UUID groupId,
            List<Media> page,
            List<Moment> moments,
            Map<UUID, UserSummary> owners,
            TimelineComments comments,
            UUID viewerId,
            GroupRole viewerRole,
            Instant windowStart,
            Instant emptyMomentsWindowEnd) {

        List<UUID> mediaIds = page.stream().map(Media::getId).toList();
        Map<UUID, List<CommentView>> previews =
                comments.previewsFor(groupId, mediaIds, viewerId, viewerRole, COMMENTS_PER_MEDIA);
        Map<UUID, Long> counts = comments.countsFor(groupId, mediaIds);
        Map<UUID, List<CategoryView>> badges = categories.badgesFor(groupId, mediaIds);

        Map<UUID, List<TimelineMedia>> byMoment = new LinkedHashMap<>();
        Map<UUID, Moment> momentsById = new LinkedHashMap<>();
        List<TimelineEntry> entries = new ArrayList<>();

        for (Media media : page) {
            TimelineMedia item = new TimelineMedia(
                    MediaView.of(media, owners.get(media.getOwnerId())),
                    counts.getOrDefault(media.getId(), 0L),
                    previews.getOrDefault(media.getId(), List.of()),
                    badges.getOrDefault(media.getId(), List.of()));

            var owner = assignment.assign(media.getCapturedAt(), moments);
            if (owner.isPresent()) {
                Moment moment = owner.get();
                momentsById.putIfAbsent(moment.getId(), moment);
                byMoment.computeIfAbsent(moment.getId(), key -> new ArrayList<>()).add(item);
            } else {
                entries.add(TimelineEntry.ofMedia(item));
            }
        }

        byMoment.forEach((momentId, mediaList) -> {
            Moment moment = momentsById.get(momentId);
            entries.add(TimelineEntry.ofMoment(block(moment, mediaList, owners, comments,
                    viewerId, viewerRole)));
        });

        // Momentos sem mídia na janela desta página entram vazios, para o título existir.
        moments.stream()
                .filter(moment -> !byMoment.containsKey(moment.getId()))
                .filter(moment -> !moment.getStartsAt().isBefore(windowStart))
                .filter(moment -> moment.getStartsAt().isBefore(emptyMomentsWindowEnd))
                .forEach(moment -> entries.add(TimelineEntry.ofMoment(
                        block(moment, List.of(), owners, comments, viewerId, viewerRole))));

        entries.sort(Comparator.comparing(TimelineEntry::at));
        return entries;
    }

    private MomentBlock block(Moment moment, List<TimelineMedia> media,
            Map<UUID, UserSummary> owners, TimelineComments comments, UUID viewerId,
            GroupRole viewerRole) {
        long total = comments.countForMoment(moment);
        return new MomentBlock(
                MomentView.of(moment, media.size(), owners.get(moment.getCreatedBy())),
                media,
                total,
                total == 0
                        ? List.of()
                        : comments.momentFeed(moment, viewerId, viewerRole, COMMENTS_PER_MOMENT));
    }
}
