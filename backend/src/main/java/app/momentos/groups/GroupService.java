package app.momentos.groups;

import app.momentos.common.exception.ConflictException;
import app.momentos.common.exception.ForbiddenException;
import app.momentos.common.exception.NotFoundException;
import app.momentos.groups.dto.CreateGroupRequest;
import app.momentos.groups.dto.GroupDetail;
import app.momentos.groups.dto.GroupMemberView;
import app.momentos.groups.dto.GroupSummary;
import app.momentos.groups.dto.PublicGroupCard;
import app.momentos.groups.dto.UpdateGroupRequest;
import app.momentos.security.GroupAccessGuard;
import app.momentos.users.User;
import app.momentos.users.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GroupService {

    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final UserRepository users;
    private final GroupAccessGuard access;

    public GroupService(GroupRepository groups, GroupMemberRepository members,
            UserRepository users, GroupAccessGuard access) {
        this.groups = groups;
        this.members = members;
        this.users = users;
        this.access = access;
    }

    @Transactional
    public GroupDetail create(CreateGroupRequest request, UUID creatorId) {
        Group group = new Group(
                Slugs.unique(request.name(), groups::existsBySlug),
                request.name().trim(),
                StringUtils.hasText(request.description()) ? request.description().trim() : null,
                request.visibility(),
                creatorId);
        if (request.theme() != null) {
            group.setTheme(request.theme());
        }
        groups.save(group);
        // Quem cria o grupo é seu primeiro administrador.
        members.save(new GroupMember(group.getId(), creatorId, GroupRole.ADMIN));
        return GroupDetail.of(group, GroupRole.ADMIN, 1);
    }

    @Transactional(readOnly = true)
    public List<GroupSummary> listMine(UUID userId) {
        List<Group> mine = groups.findAllOfMember(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> counts = memberCounts(mine.stream().map(Group::getId).toList());
        return mine.stream()
                .map(group -> GroupSummary.of(group,
                        roleOf(group.getId(), userId),
                        counts.getOrDefault(group.getId(), 0L)))
                .toList();
    }

    /** Vitrine pública: grupos abertos, visíveis a qualquer pessoa. */
    @Transactional(readOnly = true)
    public Page<PublicGroupCard> discover(String termo, Pageable pageable, UUID viewerId,
            java.util.function.BiPredicate<UUID, UUID> hasPendingRequest) {
        Page<Group> page = groups.searchOpen(termo == null ? "" : termo.trim(), pageable);
        return page.map(group -> PublicGroupCard.of(
                group,
                members.countByGroupId(group.getId()),
                viewerId != null && access.isMember(group.getId(), viewerId),
                viewerId != null && hasPendingRequest.test(group.getId(), viewerId)));
    }

    @Transactional(readOnly = true)
    public GroupDetail detail(UUID groupId, UUID userId) {
        GroupMember me = access.requireMember(groupId, userId);
        Group group = access.requireGroup(groupId);
        return GroupDetail.of(group, me.getRole(), members.countByGroupId(groupId));
    }

    @Transactional(readOnly = true)
    public Group requireBySlug(String slug) {
        return groups.findBySlug(slug).orElseThrow(() -> NotFoundException.of("Grupo"));
    }

    @Transactional
    public GroupDetail update(UUID groupId, UpdateGroupRequest request, UUID userId) {
        access.requireAdmin(groupId, userId);
        Group group = access.requireGroup(groupId);

        if (StringUtils.hasText(request.name())) {
            group.setName(request.name().trim());
        }
        if (request.description() != null) {
            group.setDescription(
                    request.description().isBlank() ? null : request.description().trim());
        }
        if (request.visibility() != null) {
            group.setVisibility(request.visibility());
        }
        if (request.theme() != null) {
            group.setTheme(request.theme());
        }
        return GroupDetail.of(group, GroupRole.ADMIN, members.countByGroupId(groupId));
    }

    @Transactional(readOnly = true)
    public List<GroupMemberView> listMembers(UUID groupId, UUID userId) {
        access.requireMember(groupId, userId);
        List<GroupMember> vinculos = members.findByGroupIdOrderByJoinedAt(groupId);
        Map<UUID, User> porId = users.findAllById(vinculos.stream().map(GroupMember::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return vinculos.stream()
                .filter(m -> porId.containsKey(m.getUserId()))
                .map(m -> GroupMemberView.of(m, porId.get(m.getUserId())))
                // Administradores primeiro, depois por ordem de entrada.
                .sorted(Comparator.comparing((GroupMemberView v) -> v.role() == GroupRole.ADMIN ? 0 : 1)
                        .thenComparing(GroupMemberView::joinedAt))
                .toList();
    }

    @Transactional
    public GroupMemberView changeRole(UUID groupId, UUID targetUserId, GroupRole role,
            UUID actorId) {
        access.requireAdmin(groupId, actorId);
        GroupMember target = members.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> NotFoundException.of("Membro"));

        // O grupo não pode ficar sem ninguém que o administre.
        if (target.isAdmin() && role == GroupRole.MEMBER && members.countAdmins(groupId) <= 1) {
            throw new ConflictException(
                    "O grupo precisa de pelo menos um administrador. Promova outra pessoa antes.");
        }
        target.setRole(role);
        return GroupMemberView.of(target, users.findById(targetUserId)
                .orElseThrow(() -> NotFoundException.of("Usuário")));
    }

    @Transactional
    public void removeMember(UUID groupId, UUID targetUserId, UUID actorId) {
        boolean saindoPorContaPropria = targetUserId.equals(actorId);
        if (saindoPorContaPropria) {
            access.requireMember(groupId, actorId);
        } else {
            access.requireAdmin(groupId, actorId);
        }

        GroupMember target = members.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> NotFoundException.of("Membro"));

        if (target.isAdmin() && members.countAdmins(groupId) <= 1) {
            throw new ConflictException(saindoPorContaPropria
                    ? "Você é o único administrador. Promova outra pessoa antes de sair."
                    : "O grupo precisa de pelo menos um administrador.");
        }
        members.delete(target);
    }

    @Transactional
    public GroupMember addMember(UUID groupId, UUID userId, GroupRole role) {
        if (members.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ConflictException("Essa pessoa já participa do grupo.");
        }
        return members.save(new GroupMember(groupId, userId, role));
    }

    private GroupRole roleOf(UUID groupId, UUID userId) {
        return members.findByGroupIdAndUserId(groupId, userId)
                .map(GroupMember::getRole)
                .orElseThrow(() -> new ForbiddenException("Você não participa deste grupo."));
    }

    private Map<UUID, Long> memberCounts(List<UUID> groupIds) {
        return members.countByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }
}
