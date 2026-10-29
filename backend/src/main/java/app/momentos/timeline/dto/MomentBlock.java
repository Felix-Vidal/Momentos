package app.momentos.timeline.dto;

import app.momentos.comments.dto.CommentView;
import app.momentos.media.dto.MediaView;
import app.momentos.moments.dto.MomentView;
import java.util.List;

/**
 * Momento com as mídias que caíram no seu intervalo.
 *
 * @param commentFeed amostra dos comentários do conjunto (do próprio Momento e das
 *                    suas mídias), usada pela caixa animada que aparece no hover do
 *                    título — vem junto para não exigir uma chamada por bloco
 */
public record MomentBlock(MomentView moment, List<MediaView> media, long commentCount,
        List<CommentView> commentFeed) {
}
