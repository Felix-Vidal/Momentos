package app.momentos.media;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    List<Media> findByOwnerIdOrderByCapturedAtDesc(UUID ownerId, Pageable pageable);

    /**
     * Mídias de um grupo em ordem cronológica, a partir de um cursor.
     *
     * <p>O cursor é o par (captura, id) do último item entregue — par, e não só a data,
     * porque várias fotos podem ter o mesmo instante de captura. Os limites usam valores
     * sentinela em vez de null para manter a consulta simples e indexável.
     */
    @Query("""
            select m from Media m
            where m.id in (select s.mediaId from MediaGroupShare s where s.groupId = :groupId)
              and m.capturedAt >= :from
              and m.capturedAt < :to
              and (m.capturedAt > :cursorAt or (m.capturedAt = :cursorAt and m.id > :cursorId))
            order by m.capturedAt asc, m.id asc
            """)
    List<Media> findSharedWithGroup(UUID groupId, Instant from, Instant to,
            Instant cursorAt, UUID cursorId, Pageable pageable);

    /**
     * Mesma página da linha do tempo, restrita às mídias marcadas com alguma das
     * categorias informadas — é o filtro por categoria da tela.
     */
    @Query("""
            select m from Media m
            where m.id in (select s.mediaId from MediaGroupShare s where s.groupId = :groupId)
              and m.id in (select mc.mediaId from MediaCategory mc
                           where mc.categoryId in :categoryIds)
              and m.capturedAt >= :from
              and m.capturedAt < :to
              and (m.capturedAt > :cursorAt or (m.capturedAt = :cursorAt and m.id > :cursorId))
            order by m.capturedAt asc, m.id asc
            """)
    List<Media> findSharedWithGroupInCategories(UUID groupId, List<UUID> categoryIds,
            Instant from, Instant to, Instant cursorAt, UUID cursorId, Pageable pageable);

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

    /** Mídias de um intervalo, para montar um Momento (usado nos blocos e nos comentários). */
    @Query("""
            select m from Media m
            where m.id in (select s.mediaId from MediaGroupShare s where s.groupId = :groupId)
              and m.capturedAt >= :startsAt
              and m.capturedAt < :endsAt
            order by m.capturedAt asc, m.id asc
            """)
    List<Media> findInInterval(UUID groupId, Instant startsAt, Instant endsAt);

    /** Contagem de mídias por dia, no fuso pedido, para a navegação por data. */
    @Query(value = """
            select to_char(date_trunc('day', m.captured_at at time zone :zone), 'YYYY-MM-DD') as dia,
                   count(*) as total
            from media m
            join media_group_shares s on s.media_id = m.id
            where s.group_id = :groupId
            group by 1
            order by 1
            """, nativeQuery = true)
    List<Object[]> countByDay(UUID groupId, String zone);
}
