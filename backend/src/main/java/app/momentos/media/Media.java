package app.momentos.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media")
public class Media {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MediaType type;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "thumbnail_key", length = 255)
    private String thumbnailKey;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    private Integer width;

    private Integer height;

    @Column(name = "duration_seconds", precision = 10, scale = 3)
    private BigDecimal durationSeconds;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "captured_at_source", nullable = false, length = 16)
    private CapturedAtSource capturedAtSource;

    @Column(length = 280)
    private String caption;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();

    protected Media() {
    }

    public Media(UUID ownerId, MediaType type, String storageKey, String mimeType, long sizeBytes,
            Instant capturedAt, CapturedAtSource capturedAtSource) {
        this.ownerId = ownerId;
        this.type = type;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.capturedAt = capturedAt;
        this.capturedAtSource = capturedAtSource;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public MediaType getType() {
        return type;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getThumbnailKey() {
        return thumbnailKey;
    }

    public void setThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public BigDecimal getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(BigDecimal durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    /** Ajuste manual da data de captura (o dono corrigindo a cronologia). */
    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
        this.capturedAtSource = CapturedAtSource.MANUAL;
    }

    public CapturedAtSource getCapturedAtSource() {
        return capturedAtSource;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
