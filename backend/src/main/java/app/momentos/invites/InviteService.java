package app.momentos.invites;

import app.momentos.common.exception.ApiException;
import app.momentos.common.exception.ConflictException;
import app.momentos.common.exception.NotFoundException;
import app.momentos.groups.Group;
import app.momentos.groups.GroupRole;
import app.momentos.groups.GroupService;
import app.momentos.invites.dto.CreateInviteRequest;
import app.momentos.invites.dto.InvitePreview;
import app.momentos.invites.dto.InviteView;
import app.momentos.security.GroupAccessGuard;
import app.momentos.users.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convites: ninguém entra em um grupo por conta própria. O token em claro só existe
 * no link entregue a quem convida; o banco guarda apenas o hash.
 */
@Service
public class InviteService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration DEFAULT_VALIDITY = Duration.ofDays(7);

    private final GroupInviteRepository invites;
    private final GroupAccessGuard access;
    private final GroupService groups;
    private final UserRepository users;

    public InviteService(GroupInviteRepository invites, GroupAccessGuard access,
            GroupService groups, UserRepository users) {
        this.invites = invites;
        this.access = access;
        this.groups = groups;
        this.users = users;
    }

    @Transactional
    public InviteView create(UUID groupId, CreateInviteRequest request, UUID adminId) {
        access.requireAdmin(groupId, adminId);

        UUID invitedUserId = null;
        String invitedEmail = null;
        if (request.email() != null && !request.email().isBlank()) {
            invitedEmail = request.email().trim();
            var invited = users.findByEmail(invitedEmail);
            if (invited.isPresent()) {
                invitedUserId = invited.get().getId();
                if (access.isMember(groupId, invitedUserId)) {
                    throw new ConflictException("Essa pessoa já participa do grupo.");
                }
            }
        }

        String token = randomToken();
        Instant expiresAt = Instant.now().plus(request.expiresInHours() == null
                ? DEFAULT_VALIDITY
                : Duration.ofHours(request.expiresInHours()));

        // Convite dirigido vale uma vez; link aberto aceita o limite informado.
        int maxUses = invitedEmail != null ? 1
                : (request.maxUses() == null ? 1 : request.maxUses());

        GroupInvite invite = invites.save(new GroupInvite(groupId, adminId, hash(token),
                request.role() == null ? GroupRole.MEMBER : request.role(),
                expiresAt, maxUses, invitedUserId, invitedEmail));

        return InviteView.of(invite, token);
    }

    @Transactional(readOnly = true)
    public List<InviteView> list(UUID groupId, UUID adminId) {
        access.requireAdmin(groupId, adminId);
        // O token não é devolvido aqui: só o hash existe no banco.
        return invites.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(invite -> InviteView.of(invite, null))
                .toList();
    }

    @Transactional
    public void revoke(UUID groupId, UUID inviteId, UUID adminId) {
        access.requireAdmin(groupId, adminId);
        GroupInvite invite = invites.findById(inviteId)
                .orElseThrow(() -> NotFoundException.of("Convite"));
        if (!invite.getGroupId().equals(groupId)) {
            throw NotFoundException.of("Convite");
        }
        invite.revoke();
    }

    /** O que o convite mostra a quem abriu o link e ainda não é membro. */
    @Transactional(readOnly = true)
    public InvitePreview preview(String token, UUID viewerId) {
        var found = invites.findByTokenHash(hash(token));
        if (found.isEmpty()) {
            return new InvitePreview(null, null, null, null, null, false, false,
                    "Este convite não existe ou já foi removido.");
        }
        GroupInvite invite = found.get();
        Group group = access.requireGroup(invite.getGroupId());
        String invitedBy = users.findById(invite.getCreatedBy())
                .map(app.momentos.users.User::getName).orElse(null);

        boolean alreadyMember = viewerId != null && access.isMember(invite.getGroupId(), viewerId);
        String reason = reasonIfUnusable(invite, viewerId);

        return new InvitePreview(group.getId(), group.getName(), group.getDescription(),
                group.getCoverMediaId(), invitedBy, reason == null, alreadyMember, reason);
    }

    @Transactional
    public UUID accept(String token, UUID userId) {
        GroupInvite invite = invites.findByTokenHash(hash(token))
                .orElseThrow(() -> NotFoundException.of("Convite"));

        String reason = reasonIfUnusable(invite, userId);
        if (reason != null) {
            throw new ApiException(HttpStatus.GONE, "convite_invalido", reason);
        }
        if (access.isMember(invite.getGroupId(), userId)) {
            throw new ConflictException("Você já participa deste grupo.");
        }

        groups.addMember(invite.getGroupId(), userId, invite.getRole());
        invite.registerUse();
        return invite.getGroupId();
    }

    @Transactional(readOnly = true)
    public List<InviteView> pendingFor(UUID userId) {
        return invites.findPendingFor(userId, Instant.now()).stream()
                .map(invite -> InviteView.of(invite, null))
                .toList();
    }

    /** Devolve a razão de o convite não servir, ou null quando ele está válido. */
    private String reasonIfUnusable(GroupInvite invite, UUID viewerId) {
        Instant now = Instant.now();
        if (invite.getRevokedAt() != null) {
            return "Este convite foi revogado.";
        }
        if (!invite.getExpiresAt().isAfter(now)) {
            return "Este convite expirou.";
        }
        if (invite.getUses() >= invite.getMaxUses()) {
            return "Este convite já foi usado.";
        }
        // Convite dirigido só vale para quem foi convidado.
        if (invite.getInvitedUserId() != null && viewerId != null
                && !invite.getInvitedUserId().equals(viewerId)) {
            return "Este convite foi enviado para outra pessoa.";
        }
        return null;
    }

    private static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", ex);
        }
    }
}
