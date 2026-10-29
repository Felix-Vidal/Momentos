package app.momentos.timeline.dto;

import app.momentos.comments.dto.CommentView;
import app.momentos.media.dto.MediaView;
import java.util.List;

/**
 * Mídia na linha do tempo com a conversa que ela já tem.
 *
 * @param comments amostra dos comentários, suficiente para a caixa animada girar
 *                 sem outra chamada; o lightbox busca a lista completa
 */
public record TimelineMedia(MediaView media, long commentCount, List<CommentView> comments) {
}
