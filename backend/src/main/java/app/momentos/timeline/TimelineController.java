package app.momentos.timeline;

import app.momentos.common.web.CursorPage;
import app.momentos.security.CurrentUser;
import app.momentos.timeline.dto.TimelineCalendar;
import app.momentos.timeline.dto.TimelineEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/timeline")
@Tag(name = "Linha do tempo")
public class TimelineController {

    private final TimelineService timeline;

    public TimelineController(TimelineService timeline) {
        this.timeline = timeline;
    }

    @GetMapping
    @Operation(summary = "Linha do tempo do grupo, em ordem cronológica e agrupada em Momentos")
    public CursorPage<TimelineEntry> load(
            @AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(defaultValue = "24") int limit) {
        return timeline.load(groupId, me.id(), from, to, cursor, Math.min(limit, 100),
                categoryIds == null ? List.of() : categoryIds);
    }

    @GetMapping("/day/{date}")
    @Operation(summary = "Linha do tempo de um dia específico, no fuso informado")
    public CursorPage<TimelineEntry> day(
            @AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId,
            @PathVariable LocalDate date,
            @RequestParam(defaultValue = "UTC") String timeZone,
            @RequestParam(defaultValue = "100") int limit) {
        Instant[] range = TimelineService.dayRange(date, zoneOf(timeZone));
        return timeline.load(groupId, me.id(), range[0], range[1], null, Math.min(limit, 200));
    }

    @GetMapping("/calendar")
    @Operation(summary = "Quantidade de mídias por mês e por dia, para a navegação por data")
    public TimelineCalendar calendar(
            @AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "UTC") String timeZone) {
        return timeline.calendar(groupId, me.id(), zoneOf(timeZone).getId());
    }

    private static ZoneId zoneOf(String timeZone) {
        try {
            return ZoneId.of(timeZone);
        } catch (RuntimeException ex) {
            return ZoneId.of("UTC");
        }
    }
}
