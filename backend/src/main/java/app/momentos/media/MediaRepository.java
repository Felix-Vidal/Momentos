package app.momentos.media;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    List<Media> findByOwnerIdOrderByCapturedAtDesc(UUID ownerId, Pageable pageable);

    /** Mídias de um grupo em ordem cronológica, para a linha do tempo. */
    @Query("""
            select m from Media m
            where m.id in (select s.mediaId from MediaGroupShare s where s.groupId = :groupId)
              and (:desde is null or m.capturedAt >= :desde)
              and (:ate is null or m.capturedAt < :ate)
            order by m.capturedAt asc, m.id asc
            """)
    List<Media> findSharedWithGroup(UUID groupId, Instant desde, Instant ate, Pageable pageable);

    /** Mídias que um integrante publicou em um grupo específico. */
    @Query("""
            select m from Media m
            where m.ownerId = :ownerId
              and m.id in (select s.mediaId from MediaGroupShare s where s.groupId = :groupId)
            order by m.capturedAt desc
            """)
    List<Media> findByOwnerInGroup(UUID ownerId, UUID groupId, Pageable pageable);

    @Query("select count(m) from Media m where m.id in "
            + "(select s.mediaId from MediaGroupShare s where s.groupId = :groupId)")
    long countInGroup(UUID groupId);
}
