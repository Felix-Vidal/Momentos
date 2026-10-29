package app.momentos.groups;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "cover_media_id")
    private UUID coverMediaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GroupVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private GroupTheme theme = GroupTheme.DEFAULT;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Group() {
    }

    public Group(String slug, String name, String description, GroupVisibility visibility,
            UUID createdBy) {
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.createdBy = createdBy;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public boolean isOpen() {
        return visibility == GroupVisibility.OPEN;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getCoverMediaId() {
        return coverMediaId;
    }

    public void setCoverMediaId(UUID coverMediaId) {
        this.coverMediaId = coverMediaId;
    }

    public GroupVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(GroupVisibility visibility) {
        this.visibility = visibility;
    }

    public GroupTheme getTheme() {
        return theme;
    }

    public void setTheme(GroupTheme theme) {
        this.theme = theme;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
