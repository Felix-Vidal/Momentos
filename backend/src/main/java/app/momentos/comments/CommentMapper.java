package app.momentos.comments;

import app.momentos.comments.dto.CommentView;
import app.momentos.groups.GroupRole;
import app.momentos.users.dto.UserSummary;
import java.util.UUID;

/** Converte comentários para a visão da API, resolvendo o que cada um pode fazer. */
final class CommentMapper {

    private CommentMapper() {
    }

    static CommentView toView(Comment comment, UserSummary author, UUID viewerId, GroupRole role) {
        boolean mine = comment.getAuthorId().equals(viewerId);
        return new CommentView(
                comment.getId(),
                comment.getGroupId(),
                comment.getMediaId(),
                comment.getMomentId(),
                comment.getBody(),
                author,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.isEdited(),
                // Só o autor edita o próprio texto; administradores podem apagar qualquer um.
                mine,
                mine || role == GroupRole.ADMIN,
                comment.getMediaId());
    }
}
