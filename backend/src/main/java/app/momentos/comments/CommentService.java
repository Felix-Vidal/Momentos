package app.momentos.comments;

import app.momentos.comments.dto.CommentView;
import app.momentos.comments.realtime.CommentEvent;
import app.momentos.comments.realtime.CommentEventPublisher;
import app.momentos.common.exception.ForbiddenException;
import app.momentos.common.exception.NotFoundException;
import app.momentos.groups.GroupMember;
import app.momentos.groups.GroupRole;
import app.momentos.media.Media;
import app.momentos.media.MediaGroupShareRepository;
import app.momentos.media.MediaService;
import app.momentos.moments.Moment;
import app.momentos.moments.MomentService;
import app.momentos.security.GroupAccessGuard;
import app.momentos.timeline.TimelineComments;
import app.momentos.users.dto.UserSummary;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService implements TimelineComments {

    private final CommentRepository comments;
    private final MediaService mediaService;
    private final MediaGroupShareRepository shares;
    private final MomentService momentService;
    private final GroupAccessGuard access;
    private final CommentEventPublisher publisher;

    public CommentService(CommentRepository comments, MediaService mediaService,
            MediaGroupShareRepository shares, MomentService momentService,
            GroupAccessGuard access, CommentEventPublisher publisher) {
        this.comments = comments;
        this.mediaService = mediaService;
        this.shares = shares;
        this.momentService = momentService;
        this.access = access;
        this.publisher = publisher;
    }

    @Transactional
    public CommentView commentOnMedia(UUID groupId, UUID mediaId, String body, UUID userId) {
        GroupMember me = access.requireMember(groupId, userId);
        // Comentar exige que a mídia realmente esteja compartilhada neste grupo.
        if (!shares.existsByMediaIdAndGroupId(mediaId, groupId)) {
            throw NotFoundException.of("Mídia neste grupo");
        }
        Comment saved = comments.save(
                Comment.onMedia(groupId, userId, body.trim(), mediaId));
        return publish(saved, me.getRole(), userId, CommentEvent.Type.CREATED);
    }

    @Transactional
    public CommentView commentOnMoment(UUID momentId, String body, UUID userId) {
        Moment moment = momentService.require(momentId);
        GroupMember me = access.requireMember(moment.getGroupId(), userId);
        Comment saved = comments.save(
                Comment.onMoment(moment.getGroupId(), userId, body.trim(), momentId));
        return publish(saved, me.getRole(), userId, CommentEvent.Type.CREATED);
    }

    @Transactional(readOnly = true)
    public List<CommentView> listForMedia(UUID groupId, UUID mediaId, UUID userId) {
        GroupMember me = access.requireMember(groupId, userId);
        List<Comment> found = comments.findByMediaIdAndGroupIdOrderByCreatedAt(mediaId, groupId);
        Map<UUID, UserSummary> authors = authorsOf(found);
        return found.stream()
                .map(comment -> CommentMapper.toView(comment, authors.get(comment.getAuthorId()),
                        userId, me.getRole()))
                .toList();
    }

    /**
     * Feed do Momento: percorre o conjunto inteiro — comentários feitos no Momento e
     * nas mídias do intervalo —, cada um já com a mídia a que se refere, para a caixa
     * animada mostrar a miniatura correspondente.
     */
    @Transactional(readOnly = true)
    public List<CommentView> listForMoment(UUID momentId, UUID userId, int limit) {
        Moment moment = momentService.require(momentId);
        GroupMember me = access.requireMember(moment.getGroupId(), userId);

        List<Comment> found = comments.findMomentFeed(moment.getGroupId(), momentId,
                moment.getStartsAt(), moment.getEndsAt(),
                org.springframework.data.domain.PageRequest.of(0, limit));
        Map<UUID, UserSummary> authors = authorsOf(found);
        return found.stream()
                .map(comment -> CommentMapper.toView(comment, authors.get(comment.getAuthorId()),
                        userId, me.getRole()))
                .toList();
    }

    @Transactional
    public CommentView edit(UUID commentId, String body, UUID userId) {
        Comment comment = require(commentId);
        GroupMember me = access.requireMember(comment.getGroupId(), userId);
        if (!comment.getAuthorId().equals(userId)) {
            throw new ForbiddenException("Só o autor pode editar o próprio comentário.");
        }
        comment.editBody(body.trim());
        return publish(comment, me.getRole(), userId, CommentEvent.Type.UPDATED);
    }

    @Transactional
    public void delete(UUID commentId, UUID userId) {
        Comment comment = require(commentId);
        GroupMember me = access.requireMember(comment.getGroupId(), userId);
        boolean mine = comment.getAuthorId().equals(userId);
        if (!mine && !me.isAdmin()) {
            throw new ForbiddenException(
                    "Só o autor ou um administrador do grupo pode apagar este comentário.");
        }
        comments.delete(comment);
        publisher.publish(comment.getGroupId(),
                new CommentEvent(CommentEvent.Type.DELETED, comment.getGroupId(),
                        comment.getId(), comment.getMediaId(), comment.getMomentId(), null));
    }

    /** Amostra de comentários por mídia, já embutida no payload da linha do tempo. */
    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<CommentView>> previewsFor(UUID groupId, List<UUID> mediaIds,
            UUID viewerId, GroupRole role, int perMedia) {
        if (mediaIds.isEmpty()) {
            return Map.of();
        }
        List<Comment> found = comments.findForMedia(groupId, mediaIds);
        Map<UUID, UserSummary> authors = authorsOf(found);

        Map<UUID, List<CommentView>> byMedia = new java.util.LinkedHashMap<>();
        for (Comment comment : found) {
            List<CommentView> list =
                    byMedia.computeIfAbsent(comment.getMediaId(), key -> new java.util.ArrayList<>());
            if (list.size() < perMedia) {
                list.add(CommentMapper.toView(comment, authors.get(comment.getAuthorId()),
                        viewerId, role));
            }
        }
        return byMedia;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> countsFor(UUID groupId, List<UUID> mediaIds) {
        if (mediaIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new java.util.HashMap<>();
        for (Comment comment : comments.findForMedia(groupId, mediaIds)) {
            counts.merge(comment.getMediaId(), 1L, Long::sum);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public long countForMoment(Moment moment) {
        return comments.countMomentFeed(moment.getGroupId(), moment.getId(),
                moment.getStartsAt(), moment.getEndsAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentView> momentFeed(Moment moment, UUID viewerId, GroupRole role, int limit) {
        List<Comment> found = comments.findMomentFeed(moment.getGroupId(), moment.getId(),
                moment.getStartsAt(), moment.getEndsAt(),
                org.springframework.data.domain.PageRequest.of(0, limit));
        Map<UUID, UserSummary> authors = authorsOf(found);
        return found.stream()
                .map(comment -> CommentMapper.toView(comment, authors.get(comment.getAuthorId()),
                        viewerId, role))
                .toList();
    }

    private Comment require(UUID commentId) {
        return comments.findById(commentId)
                .orElseThrow(() -> NotFoundException.of("Comentário"));
    }

    private Map<UUID, UserSummary> authorsOf(List<Comment> found) {
        List<UUID> ids = found.stream().map(Comment::getAuthorId).distinct().toList();
        return ids.isEmpty() ? Map.of() : mediaService.summariesOf(ids);
    }

    private CommentView publish(Comment comment, GroupRole role, UUID userId,
            CommentEvent.Type type) {
        CommentView view = CommentMapper.toView(comment,
                mediaService.summaryOf(comment.getAuthorId()), userId, role);
        publisher.publish(comment.getGroupId(), new CommentEvent(type, comment.getGroupId(),
                comment.getId(), comment.getMediaId(), comment.getMomentId(), view));
        return view;
    }

    /** Usado pelo lightbox para saber a qual mídia um comentário pertence. */
    @Transactional(readOnly = true)
    public Media requireVisibleMedia(UUID mediaId, UUID userId) {
        return mediaService.requireVisible(mediaId, userId);
    }
}
