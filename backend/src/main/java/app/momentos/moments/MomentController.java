package app.momentos.moments;

import app.momentos.moments.dto.CreateMomentRequest;
import app.momentos.moments.dto.MomentView;
import app.momentos.moments.dto.UpdateMomentRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Momentos")
public class MomentController {

    private final MomentService momentService;

    public MomentController(MomentService momentService) {
        this.momentService = momentService;
    }

    @PostMapping("/api/groups/{groupId}/moments")
    @Operation(summary = "Cria um momento com título a partir de um intervalo de data e hora")
    public ResponseEntity<MomentView> create(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId, @Valid @RequestBody CreateMomentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(momentService.create(groupId, request, me.id()));
    }

    @GetMapping("/api/groups/{groupId}/moments")
    @Operation(summary = "Momentos do grupo, do mais antigo para o mais recente")
    public List<MomentView> list(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId) {
        return momentService.list(groupId, me.id());
    }

    @GetMapping("/api/moments/{momentId}")
    @Operation(summary = "Detalhe de um momento")
    public MomentView detail(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID momentId) {
        return momentService.detail(momentId, me.id());
    }

    @PatchMapping("/api/moments/{momentId}")
    @Operation(summary = "Edita título, descrição ou intervalo (autor ou administrador)")
    public MomentView update(@AuthenticationPrincipal CurrentUser me, @PathVariable UUID momentId,
            @Valid @RequestBody UpdateMomentRequest request) {
        return momentService.update(momentId, request, me.id());
    }

    @DeleteMapping("/api/moments/{momentId}")
    @Operation(summary = "Apaga o momento; as mídias voltam a aparecer soltas")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID momentId) {
        momentService.delete(momentId, me.id());
        return ResponseEntity.noContent().build();
    }
}
