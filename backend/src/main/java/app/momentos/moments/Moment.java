package app.momentos.moments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Agrupamento com título definido por um intervalo de data e hora.
 *
 * <p>Exemplo: "Corujão da LetalCompany", de 13/10 às 22:00 a 14/10 às 06:00 — toda
 * mídia capturada nesse intervalo aparece sob esse título na linha do tempo.
 */
@Entity
@Table(name = "moments")
public class Moment {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Moment() {
    }

    public Moment(UUID groupId, String title, String description, Instant startsAt, Instant endsAt,
            UUID createdBy) {
        this.groupId = groupId;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdBy = createdBy;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    /** Intervalo meio-aberto: o início pertence ao Momento, o fim já não. */
    public boolean covers(Instant instant) {
        return !instant.isBefore(startsAt) && instant.isBefore(endsAt);
    }

    public Duration duration() {
        return Duration.between(startsAt, endsAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
