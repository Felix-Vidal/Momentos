package app.momentos.categories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Marcação de uma mídia com uma categoria. */
@Entity
@Table(name = "media_categories")
public class MediaCategory {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "tagged_by", nullable = false)
    private UUID taggedBy;

    @Column(name = "tagged_at", nullable = false)
    private Instant taggedAt = Instant.now();

    protected MediaCategory() {
    }

    public MediaCategory(UUID mediaId, UUID categoryId, UUID taggedBy) {
        this.mediaId = mediaId;
        this.categoryId = categoryId;
        this.taggedBy = taggedBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }
}
