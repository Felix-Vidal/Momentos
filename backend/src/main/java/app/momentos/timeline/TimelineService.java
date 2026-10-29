package app.momentos.timeline;

import app.momentos.common.web.CursorPage;
import app.momentos.media.Media;
import app.momentos.media.MediaRepository;
import app.momentos.media.MediaService;
import app.momentos.moments.Moment;
import app.momentos.moments.MomentRepository;
import app.momentos.security.GroupAccessGuard;
import app.momentos.timeline.dto.TimelineCalendar;
import app.momentos.timeline.dto.TimelineEntry;
import app.momentos.users.dto.UserSummary;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta a linha do tempo do grupo: mídias em ordem cronológica, agrupadas em Momentos. */
@Service
public class TimelineService {

    /** Limites sentinela, para as consultas não precisarem de parâmetros nulos. */
    private static final Instant INICIO_DOS_TEMPOS = Instant.EPOCH;
    private static final Instant FIM_DOS_TEMPOS = Instant.parse("9999-12-31T23:59:59Z");
    private static final UUID PRIMEIRO_ID = new UUID(0, 0);

    private final MediaRepository media;
    private final MomentRepository moments;
    private final MediaService mediaService;
    private final TimelineAssembler assembler;
    private final GroupAccessGuard access;

    public TimelineService(MediaRepository media, MomentRepository moments,
            MediaService mediaService, TimelineAssembler assembler, GroupAccessGuard access) {
        this.media = media;
        this.moments = moments;
        this.mediaService = mediaService;
        this.assembler = assembler;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public CursorPage<TimelineEntry> load(UUID groupId, UUID userId, Instant from, Instant to,
            String cursor, int limit, Function<Moment, Long> commentCountOf) {
        access.requireMember(groupId, userId);

        Cursor position = Cursor.decode(cursor);
        Instant start = from == null ? INICIO_DOS_TEMPOS : from;
        Instant end = to == null ? FIM_DOS_TEMPOS : to;

        // Busca um item extra para saber se existe página seguinte.
        List<Media> found = media.findSharedWithGroup(groupId, start, end,
                position.at(), position.id(), PageRequest.of(0, limit + 1));

        boolean hasMore = found.size() > limit;
        List<Media> page = hasMore ? found.subList(0, limit) : found;

        Instant windowStart = position.at().equals(INICIO_DOS_TEMPOS)
                ? start
                : position.at();
        Instant windowEnd = hasMore ? found.get(limit).getCapturedAt() : end;

        List<Moment> relevantMoments = moments.findByGroupIdOrderByStartsAt(groupId);
        Map<UUID, UserSummary> owners = ownersOf(page, relevantMoments);

        List<TimelineEntry> entries = assembler.assemble(page, relevantMoments, owners,
                commentCountOf, windowStart, windowEnd);

        if (!hasMore) {
            return CursorPage.last(entries);
        }
        Media last = page.getLast();
        return new CursorPage<>(entries,
                new Cursor(last.getCapturedAt(), last.getId()).encode(), true);
    }

    /** Contagens por dia e mês, para a navegação por data. */
    @Transactional(readOnly = true)
    public TimelineCalendar calendar(UUID groupId, UUID userId, String zone) {
        access.requireMember(groupId, userId);

        Map<String, List<TimelineCalendar.Day>> byMonth = new LinkedHashMap<>();
        long total = 0;

        for (Object[] row : media.countByDay(groupId, zone)) {
            String date = (String) row[0];
            long count = ((Number) row[1]).longValue();
            total += count;
            byMonth.computeIfAbsent(date.substring(0, 7), key -> new ArrayList<>())
                    .add(new TimelineCalendar.Day(date, count));
        }

        List<TimelineCalendar.Month> months = byMonth.entrySet().stream()
                .map(entry -> new TimelineCalendar.Month(
                        entry.getKey(),
                        entry.getValue().stream().mapToLong(TimelineCalendar.Day::count).sum(),
                        entry.getValue()))
                .toList();
        return new TimelineCalendar(months, total);
    }

    /** Converte um dia (no fuso do cliente) no intervalo a consultar. */
    public static Instant[] dayRange(LocalDate day, java.time.ZoneId zone) {
        return new Instant[] {
                day.atStartOfDay(zone).toInstant(),
                day.plusDays(1).atStartOfDay(zone).toInstant()};
    }

    private Map<UUID, UserSummary> ownersOf(List<Media> page, List<Moment> moments) {
        List<UUID> ids = Stream.concat(
                        page.stream().map(Media::getOwnerId),
                        moments.stream().map(Moment::getCreatedBy))
                .distinct()
                .toList();
        return ids.isEmpty() ? Map.of() : mediaService.summariesOf(ids);
    }

    /**
     * Posição na linha do tempo. O par (captura, id) evita repetir ou pular itens
     * quando várias mídias compartilham o mesmo instante.
     */
    record Cursor(Instant at, UUID id) {

        static Cursor decode(String encoded) {
            if (encoded == null || encoded.isBlank()) {
                return new Cursor(INICIO_DOS_TEMPOS, PRIMEIRO_ID);
            }
            try {
                String raw = new String(Base64.getUrlDecoder().decode(encoded),
                        StandardCharsets.UTF_8);
                String[] parts = raw.split("\\|", 2);
                return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
            } catch (RuntimeException ex) {
                // Cursor inválido recomeça do início, em vez de derrubar a requisição.
                return new Cursor(INICIO_DOS_TEMPOS, PRIMEIRO_ID);
            }
        }

        String encode() {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString((at.toString() + "|" + id).getBytes(StandardCharsets.UTF_8));
        }
    }
}
