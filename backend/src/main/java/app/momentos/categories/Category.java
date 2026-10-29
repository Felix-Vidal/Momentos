package app.momentos.categories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Etiqueta criada pelo grupo para classificar fotos e vídeos — "almoço", "tereré",
 * "corujão". Cada grupo tem o seu conjunto.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(nullable = false, length = 40)
    private String name;

    /** Cor em hexadecimal (#RRGGBB), usada no selo e no filtro. */
    @Column(nullable = false, length = 7)
    private String color;

    @Column(length = 16)
    private String emoji;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Category() {
    }

    public Category(UUID groupId, String name, String color, String emoji, UUID createdBy) {
        this.groupId = groupId;
        this.name = name;
        this.color = color;
        this.emoji = emoji;
        this.createdBy = createdBy;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
