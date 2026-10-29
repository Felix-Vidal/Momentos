package app.momentos.comments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Comentário de um membro em uma mídia ou em um Momento.
 *
 * <p>Sempre carrega o grupo: a mesma foto pode estar compartilhada em vários grupos,
 * e cada grupo tem a sua conversa.
 */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "media_id")
    private UUID mediaId;

    @Column(name = "moment_id")
    private UUID momentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Comment() {
    }

    private Comment(UUID groupId, UUID authorId, String body, UUID mediaId, UUID momentId) {
        this.groupId = groupId;
        this.authorId = authorId;
        this.body = body;
        this.mediaId = mediaId;
        this.momentId = momentId;
    }

    public static Comment onMedia(UUID groupId, UUID authorId, String body, UUID mediaId) {
        return new Comment(groupId, authorId, body, mediaId, null);
    }

    public static Comment onMoment(UUID groupId, UUID authorId, String body, UUID momentId) {
        return new Comment(groupId, authorId, body, null, momentId);
    }

    public void editBody(String body) {
        this.body = body;
        this.updatedAt = Instant.now();
    }

    /** Foi editado se a atualização veio depois da criação (com folga de um segundo). */
    public boolean isEdited() {
        return updatedAt.isAfter(createdAt.plusSeconds(1));
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public UUID getMomentId() {
        return momentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
