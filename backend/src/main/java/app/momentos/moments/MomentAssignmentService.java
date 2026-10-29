package app.momentos.moments;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Decide a qual Momento uma mídia pertence.
 *
 * <p>A regra é só uma: o instante de captura tem de cair no intervalo meio-aberto
 * {@code [startsAt, endsAt)}. Quando Momentos se sobrepõem — por exemplo "Corujão"
 * (22:00–06:00) dentro de "Viagem" (três dias) — vale o <strong>mais específico</strong>,
 * isto é, o de menor duração; em caso de empate, o criado mais recentemente, que
 * reflete a intenção mais atual de quem organizou a linha do tempo.
 */
public class MomentAssignmentService {

    /** Mais específico primeiro: menor duração e, no empate, criado depois. */
    private static final Comparator<Moment> MAIS_ESPECIFICO =
            Comparator.comparing(Moment::duration)
                    .thenComparing(Moment::getCreatedAt, Comparator.reverseOrder());

    public Optional<Moment> assign(Instant capturedAt, List<Moment> candidates) {
        if (capturedAt == null || candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(moment -> moment.covers(capturedAt))
                .min(MAIS_ESPECIFICO);
    }
}
