package app.momentos.moments;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MomentRepository extends JpaRepository<Moment, UUID> {

    List<Moment> findByGroupIdOrderByStartsAt(UUID groupId);

    /** Momentos que tocam o intervalo pedido (para montar um trecho da linha do tempo). */
    @Query("""
            select m from Moment m
            where m.groupId = :groupId
              and m.endsAt > :desde
              and m.startsAt < :ate
            order by m.startsAt
            """)
    List<Moment> findOverlapping(UUID groupId, Instant desde, Instant ate);
}
