package app.momentos.comments.dto;

import app.momentos.users.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;

/**
 * Comentário como a interface consome.
 *
 * @param mediaId       preenchido quando o comentário é de uma mídia
 * @param momentId      preenchido quando o comentário é do Momento como um todo
 * @param mediaThumbnail miniatura da mídia comentada, usada no feed do título do
 *                       Momento para mostrar a que foto cada comentário se refere
 */
public record CommentView(
        UUID id,
        UUID groupId,
        UUID mediaId,
        UUID momentId,
        String body,
        UserSummary author,
        Instant createdAt,
        Instant updatedAt,
        boolean edited,
        boolean canEdit,
        boolean canDelete,
        UUID mediaThumbnail
) {
}
