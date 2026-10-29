package app.momentos.invites;

import app.momentos.groups.GroupRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Convite para entrar em um grupo.
 *
 * <p>Pode ser um link compartilhável (com limite de usos e validade) ou um convite
 * dirigido a uma pessoa específica. Só o hash do token fica no banco.
 */
@Entity
@Table(name = "group_invites")
public class GroupInvite {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "invited_user_id")
    private UUID invitedUserId;

    @Column(name = "invited_email", length = 160)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GroupRole role = GroupRole.MEMBER;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    @Column(nullable = false)
    private int uses;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected GroupInvite() {
    }

    public GroupInvite(UUID groupId, UUID createdBy, String tokenHash, GroupRole role,
            Instant expiresAt, int maxUses, UUID invitedUserId, String invitedEmail) {
        this.groupId = groupId;
        this.createdBy = createdBy;
        this.tokenHash = tokenHash;
        this.role = role;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.invitedUserId = invitedUserId;
        this.invitedEmail = invitedEmail;
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now) && uses < maxUses;
    }

    public void registerUse() {
        this.uses++;
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getInvitedUserId() {
        return invitedUserId;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public GroupRole getRole() {
        return role;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getUses() {
        return uses;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
