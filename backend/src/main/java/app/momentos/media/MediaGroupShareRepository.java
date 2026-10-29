package app.momentos.media;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaGroupShareRepository extends JpaRepository<MediaGroupShare, UUID> {

    List<MediaGroupShare> findByMediaId(UUID mediaId);

    Optional<MediaGroupShare> findByMediaIdAndGroupId(UUID mediaId, UUID groupId);

    boolean existsByMediaIdAndGroupId(UUID mediaId, UUID groupId);

    /** Grupos em que a mídia está compartilhada e dos quais o usuário participa. */
    @Query("""
            select count(s) > 0 from MediaGroupShare s
            where s.mediaId = :mediaId
              and s.groupId in (select m.groupId from GroupMember m where m.userId = :userId)
            """)
    boolean isVisibleTo(UUID mediaId, UUID userId);
}
