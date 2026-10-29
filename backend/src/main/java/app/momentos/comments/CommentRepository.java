package app.momentos.comments;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByMediaIdAndGroupIdOrderByCreatedAt(UUID mediaId, UUID groupId);

    List<Comment> findByMomentIdOrderByCreatedAt(UUID momentId);

    long countByMediaIdAndGroupId(UUID mediaId, UUID groupId);

    long countByMomentId(UUID momentId);

    /** Comentários das mídias listadas, em uma consulta só (evita N+1 na linha do tempo). */
    @Query("""
            select c from Comment c
            where c.groupId = :groupId and c.mediaId in :mediaIds
            order by c.mediaId, c.createdAt
            """)
    List<Comment> findForMedia(UUID groupId, List<UUID> mediaIds);

    /**
     * Feed do Momento: o que foi dito no próprio Momento mais o que foi dito nas
     * mídias capturadas dentro do intervalo dele.
     */
    @Query("""
            select c from Comment c
            where c.groupId = :groupId
              and (c.momentId = :momentId
                   or c.mediaId in (
                        select m.id from Media m
                        where m.capturedAt >= :startsAt and m.capturedAt < :endsAt
                          and m.id in (select s.mediaId from MediaGroupShare s
                                       where s.groupId = :groupId)))
            order by c.createdAt
            """)
    List<Comment> findMomentFeed(UUID groupId, UUID momentId, Instant startsAt, Instant endsAt,
            Pageable pageable);

    @Query("""
            select count(c) from Comment c
            where c.groupId = :groupId
              and (c.momentId = :momentId
                   or c.mediaId in (
                        select m.id from Media m
                        where m.capturedAt >= :startsAt and m.capturedAt < :endsAt
                          and m.id in (select s.mediaId from MediaGroupShare s
                                       where s.groupId = :groupId)))
            """)
    long countMomentFeed(UUID groupId, UUID momentId, Instant startsAt, Instant endsAt);
}
