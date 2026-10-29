package app.momentos.invites;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Pedido para entrar em um grupo aberto, sujeito à aprovação de um administrador. */
@Entity
@Table(name = "join_requests")
public class JoinRequest {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JoinRequestStatus status = JoinRequestStatus.PENDING;

    @Column(length = 280)
    private String message;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected JoinRequest() {
    }

    public JoinRequest(UUID groupId, UUID userId, String message) {
        this.groupId = groupId;
        this.userId = userId;
        this.message = message;
    }

    public void decide(JoinRequestStatus status, UUID adminId) {
        this.status = status;
        this.decidedBy = adminId;
        this.decidedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getUserId() {
        return userId;
    }

    public JoinRequestStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
