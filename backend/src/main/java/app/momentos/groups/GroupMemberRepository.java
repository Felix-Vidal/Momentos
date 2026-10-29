package app.momentos.groups;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

    List<GroupMember> findByGroupIdOrderByJoinedAt(UUID groupId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    long countByGroupId(UUID groupId);

    @Query("select m.groupId, count(m) from GroupMember m where m.groupId in :groupIds group by m.groupId")
    List<Object[]> countByGroupIds(List<UUID> groupIds);

    @Query("select count(m) from GroupMember m where m.groupId = :groupId and m.role = app.momentos.groups.GroupRole.ADMIN")
    long countAdmins(UUID groupId);
}
