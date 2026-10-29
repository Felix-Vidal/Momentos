package app.momentos.comments.realtime;

import app.momentos.comments.dto.CommentView;
import java.util.UUID;

/**
 * Aviso enviado pelo WebSocket quando a conversa muda, para as telas abertas
 * acompanharem sem recarregar.
 *
 * @param comment o comentário; nulo quando o evento é de exclusão
 */
public record CommentEvent(
        Type type,
        UUID groupId,
        UUID commentId,
        UUID mediaId,
        UUID momentId,
        CommentView comment
) {
    public enum Type {
        CREATED,
        UPDATED,
        DELETED
    }
}
