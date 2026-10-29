package app.momentos.invites;

import app.momentos.common.exception.ConflictException;
import app.momentos.common.exception.ForbiddenException;
import app.momentos.common.exception.NotFoundException;
import app.momentos.groups.Group;
import app.momentos.groups.GroupRole;
import app.momentos.groups.GroupService;
import app.momentos.invites.dto.JoinRequestView;
import app.momentos.security.GroupAccessGuard;
import app.momentos.users.User;
import app.momentos.users.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Solicitações de entrada em grupos abertos. A aprovação é sempre de um
 * administrador: ninguém entra automaticamente.
 */
@Service
public class JoinRequestService {

    private final JoinRequestRepository requests;
    private final GroupAccessGuard access;
    private final GroupService groups;
    private final UserRepository users;

    public JoinRequestService(JoinRequestRepository requests, GroupAccessGuard access,
            GroupService groups, UserRepository users) {
        this.requests = requests;
        this.access = access;
        this.groups = groups;
        this.users = users;
    }

    @Transactional
    public void ask(UUID groupId, UUID userId, String message) {
        Group group = access.requireGroup(groupId);
        // Grupo privado não aparece para quem está fora, e não aceita pedidos.
        if (!group.isOpen()) {
            throw NotFoundException.of("Grupo");
        }
        if (access.isMember(groupId, userId)) {
            throw new ConflictException("Você já participa deste grupo.");
        }
        if (requests.hasPending(groupId, userId)) {
            throw new ConflictException("Você já tem um pedido aguardando resposta.");
        }
        requests.save(new JoinRequest(groupId, userId,
                StringUtils.hasText(message) ? message.trim() : null));
    }

    @Transactional(readOnly = true)
    public List<JoinRequestView> pending(UUID groupId, UUID adminId) {
        access.requireAdmin(groupId, adminId);
        List<JoinRequest> pending =
                requests.findByGroupIdAndStatusOrderByCreatedAt(groupId, JoinRequestStatus.PENDING);
        Map<UUID, User> byId = users.findAllById(
                        pending.stream().map(JoinRequest::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return pending.stream()
                .map(request -> {
                    User user = byId.get(request.getUserId());
                    return JoinRequestView.of(request,
                            user == null ? "Usuário removido" : user.getName(),
                            user == null ? null : user.getAvatarMediaId());
                })
                .toList();
    }

    @Transactional
    public void approve(UUID groupId, UUID requestId, UUID adminId) {
        JoinRequest request = requirePending(groupId, requestId, adminId);
        groups.addMember(groupId, request.getUserId(), GroupRole.MEMBER);
        request.decide(JoinRequestStatus.APPROVED, adminId);
    }

    @Transactional
    public void reject(UUID groupId, UUID requestId, UUID adminId) {
        requirePending(groupId, requestId, adminId).decide(JoinRequestStatus.REJECTED, adminId);
    }

    @Transactional
    public void cancelOwn(UUID groupId, UUID userId) {
        JoinRequest request = requests
                .findByGroupIdAndUserIdAndStatus(groupId, userId, JoinRequestStatus.PENDING)
                .orElseThrow(() -> NotFoundException.of("Pedido"));
        requests.delete(request);
    }

    private JoinRequest requirePending(UUID groupId, UUID requestId, UUID adminId) {
        access.requireAdmin(groupId, adminId);
        JoinRequest request = requests.findById(requestId)
                .orElseThrow(() -> NotFoundException.of("Pedido"));
        if (!request.getGroupId().equals(groupId)) {
            throw NotFoundException.of("Pedido");
        }
        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new ForbiddenException("Este pedido já foi respondido.");
        }
        return request;
    }
}
