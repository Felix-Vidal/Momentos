package app.momentos.timeline.dto;

import java.util.List;

/**
 * Resumo para a navegação por data: quantas mídias existem em cada mês e dia,
 * no fuso de quem está olhando.
 */
public record TimelineCalendar(List<Month> months, long total) {

    public record Month(String month, long count, List<Day> days) {
    }

    public record Day(String date, long count) {
    }
}
