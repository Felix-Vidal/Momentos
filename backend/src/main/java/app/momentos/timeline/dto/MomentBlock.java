package app.momentos.timeline.dto;

import app.momentos.comments.dto.CommentView;
import app.momentos.moments.dto.MomentView;
import java.util.List;

/**
 * Momento com as mídias que caíram no seu intervalo.
 *
 * @param commentFeed comentários do conjunto (do próprio Momento e de todas as suas
 *                    mídias), que a caixa animada percorre no hover do título
 */
public record MomentBlock(MomentView moment, List<TimelineMedia> media, long commentCount,
        List<CommentView> commentFeed) {
}
