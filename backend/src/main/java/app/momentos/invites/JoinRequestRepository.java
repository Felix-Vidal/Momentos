package app.momentos.invites;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, UUID> {

    Optional<JoinRequest> findByGroupIdAndUserIdAndStatus(UUID groupId, UUID userId,
            JoinRequestStatus status);

    List<JoinRequest> findByGroupIdAndStatusOrderByCreatedAt(UUID groupId,
            JoinRequestStatus status);

    @Query("""
            select count(r) > 0 from JoinRequest r
            where r.groupId = :groupId and r.userId = :userId
              and r.status = app.momentos.invites.JoinRequestStatus.PENDING
            """)
    boolean hasPending(UUID groupId, UUID userId);
}
