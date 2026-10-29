package app.momentos.security;

import app.momentos.common.exception.ForbiddenException;
import app.momentos.common.exception.NotFoundException;
import app.momentos.groups.Group;
import app.momentos.groups.GroupMember;
import app.momentos.groups.GroupMemberRepository;
import app.momentos.groups.GroupRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponto único das decisões de acesso a um grupo.
 *
 * <p>Conteúdo (mídias, Momentos e comentários) é visível apenas a membros, tanto em
 * grupos abertos quanto privados. Para quem não é membro, um grupo privado responde
 * 404 em vez de 403: confirmar a existência já seria vazar informação.
 */
@Component
public class GroupAccessGuard {

    private final GroupRepository groups;
    private final GroupMemberRepository members;

    public GroupAccessGuard(GroupRepository groups, GroupMemberRepository members) {
        this.groups = groups;
        this.members = members;
    }

    @Transactional(readOnly = true)
    public Group requireGroup(UUID groupId) {
        return groups.findById(groupId).orElseThrow(() -> NotFoundException.of("Grupo"));
    }

    /** Garante que o usuário é membro e devolve o vínculo (com o papel). */
    @Transactional(readOnly = true)
    public GroupMember requireMember(UUID groupId, UUID userId) {
        Group group = requireGroup(groupId);
        return members.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> group.isOpen()
                        ? new ForbiddenException(
                                "Só membros veem o conteúdo deste grupo. Peça para entrar.")
                        : NotFoundException.of("Grupo"));
    }

    @Transactional(readOnly = true)
    public GroupMember requireAdmin(UUID groupId, UUID userId) {
        GroupMember member = requireMember(groupId, userId);
        if (!member.isAdmin()) {
            throw new ForbiddenException("Só administradores do grupo podem fazer isso.");
        }
        return member;
    }

    @Transactional(readOnly = true)
    public boolean isMember(UUID groupId, UUID userId) {
        return members.existsByGroupIdAndUserId(groupId, userId);
    }
}
