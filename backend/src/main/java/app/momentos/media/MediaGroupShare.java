package app.momentos.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Compartilhamento de uma mídia com um grupo: é isso que a monta na linha do tempo. */
@Entity
@Table(name = "media_group_shares")
public class MediaGroupShare {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "shared_by", nullable = false)
    private UUID sharedBy;

    @Column(name = "shared_at", nullable = false)
    private Instant sharedAt = Instant.now();

    protected MediaGroupShare() {
    }

    public MediaGroupShare(UUID mediaId, UUID groupId, UUID sharedBy) {
        this.mediaId = mediaId;
        this.groupId = groupId;
        this.sharedBy = sharedBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getSharedBy() {
        return sharedBy;
    }

    public Instant getSharedAt() {
        return sharedAt;
    }
}
