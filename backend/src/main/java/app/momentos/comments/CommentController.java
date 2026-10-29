package app.momentos.comments;

import app.momentos.comments.dto.CommentView;
import app.momentos.comments.dto.CreateCommentRequest;
import app.momentos.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Comentários")
public class CommentController {

    private final CommentService comments;

    public CommentController(CommentService comments) {
        this.comments = comments;
    }

    @GetMapping("/api/media/{mediaId}/comments")
    @Operation(summary = "Comentários de uma mídia dentro de um grupo")
    public List<CommentView> ofMedia(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID mediaId, @RequestParam UUID groupId) {
        return comments.listForMedia(groupId, mediaId, me.id());
    }

    @PostMapping("/api/media/{mediaId}/comments")
    @Operation(summary = "Comenta em uma foto ou vídeo")
    public ResponseEntity<CommentView> onMedia(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID mediaId, @RequestParam UUID groupId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comments.commentOnMedia(groupId, mediaId, request.body(), me.id()));
    }

    @GetMapping("/api/moments/{momentId}/comments")
    @Operation(summary = "Comentários do momento e de todas as mídias do intervalo dele")
    public List<CommentView> ofMoment(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID momentId,
            @RequestParam(defaultValue = "60") int limit) {
        return comments.listForMoment(momentId, me.id(), Math.min(limit, 200));
    }

    @PostMapping("/api/moments/{momentId}/comments")
    @Operation(summary = "Comenta no momento como um todo")
    public ResponseEntity<CommentView> onMoment(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID momentId, @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comments.commentOnMoment(momentId, request.body(), me.id()));
    }

    @PatchMapping("/api/comments/{commentId}")
    @Operation(summary = "Edita o próprio comentário")
    public CommentView edit(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID commentId, @Valid @RequestBody CreateCommentRequest request) {
        return comments.edit(commentId, request.body(), me.id());
    }

    @DeleteMapping("/api/comments/{commentId}")
    @Operation(summary = "Apaga um comentário (autor ou administrador do grupo)")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID commentId) {
        comments.delete(commentId, me.id());
        return ResponseEntity.noContent().build();
    }
}
