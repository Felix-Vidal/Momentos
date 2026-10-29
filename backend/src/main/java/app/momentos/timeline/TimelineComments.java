package app.momentos.timeline;

import app.momentos.comments.dto.CommentView;
import app.momentos.groups.GroupRole;
import app.momentos.moments.Moment;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O que a linha do tempo precisa saber sobre comentários.
 *
 * <p>Existe como interface para a montagem da linha do tempo não depender dos detalhes
 * do módulo de comentários — e para ficar explícito que tudo é resolvido em consultas
 * agregadas, sem uma ida ao banco por mídia.
 */
public interface TimelineComments {

    /** Amostra de comentários por mídia, para a caixa animada abrir já com conteúdo. */
    Map<UUID, List<CommentView>> previewsFor(UUID groupId, List<UUID> mediaIds, UUID viewerId,
            GroupRole role, int perMedia);

    /** Total de comentários por mídia. */
    Map<UUID, Long> countsFor(UUID groupId, List<UUID> mediaIds);

    /** Total do conjunto de um Momento: o próprio Momento mais as mídias do intervalo. */
    long countForMoment(Moment moment);

    /** Feed do conjunto de um Momento, cada item com a mídia a que se refere. */
    List<CommentView> momentFeed(Moment moment, UUID viewerId, GroupRole role, int limit);
}
