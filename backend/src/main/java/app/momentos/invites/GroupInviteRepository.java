package app.momentos.invites;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, UUID> {

    Optional<GroupInvite> findByTokenHash(String tokenHash);

    List<GroupInvite> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

    /** Convites dirigidos a uma pessoa e ainda válidos. */
    @Query("""
            select i from GroupInvite i
            where i.invitedUserId = :userId
              and i.revokedAt is null
              and i.expiresAt > :agora
              and i.uses < i.maxUses
            order by i.createdAt desc
            """)
    List<GroupInvite> findPendingFor(UUID userId, Instant agora);
}
