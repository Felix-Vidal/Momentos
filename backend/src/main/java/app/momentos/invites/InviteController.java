package app.momentos.invites;

import app.momentos.invites.dto.CreateInviteRequest;
import app.momentos.invites.dto.CreateJoinRequest;
import app.momentos.invites.dto.InvitePreview;
import app.momentos.invites.dto.InviteView;
import app.momentos.invites.dto.JoinRequestView;
import app.momentos.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Convites e solicitações")
public class InviteController {

    private final InviteService invites;
    private final JoinRequestService joinRequests;

    public InviteController(InviteService invites, JoinRequestService joinRequests) {
        this.invites = invites;
        this.joinRequests = joinRequests;
    }

    // --- convites ---

    @PostMapping("/api/groups/{groupId}/invites")
    @Operation(summary = "Gera um convite: link compartilhável ou dirigido a um e-mail")
    public ResponseEntity<InviteView> create(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId, @Valid @RequestBody CreateInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invites.create(groupId, request, me.id()));
    }

    @GetMapping("/api/groups/{groupId}/invites")
    @Operation(summary = "Convites do grupo (administradores); sem o token em claro")
    public List<InviteView> list(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId) {
        return invites.list(groupId, me.id());
    }

    @DeleteMapping("/api/groups/{groupId}/invites/{inviteId}")
    @Operation(summary = "Revoga um convite")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId, @PathVariable UUID inviteId) {
        invites.revoke(groupId, inviteId, me.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/invites/{token}")
    @Operation(summary = "Mostra de qual grupo é o convite, antes de aceitar")
    public InvitePreview preview(@AuthenticationPrincipal CurrentUser me,
            @PathVariable String token) {
        return invites.preview(token, me == null ? null : me.id());
    }

    @PostMapping("/api/invites/{token}/accept")
    @Operation(summary = "Aceita o convite e entra no grupo")
    public Map<String, UUID> accept(@AuthenticationPrincipal CurrentUser me,
            @PathVariable String token) {
        return Map.of("groupId", invites.accept(token, me.id()));
    }

    @GetMapping("/api/invites/mine")
    @Operation(summary = "Convites dirigidos a mim e ainda válidos")
    public List<InviteView> mine(@AuthenticationPrincipal CurrentUser me) {
        return invites.pendingFor(me.id());
    }

    // --- solicitações de entrada ---

    @PostMapping("/api/groups/{groupId}/join-requests")
    @Operation(summary = "Pede para entrar em um grupo aberto")
    public ResponseEntity<Void> ask(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId, @Valid @RequestBody CreateJoinRequest request) {
        joinRequests.ask(groupId, me.id(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/groups/{groupId}/join-requests")
    @Operation(summary = "Pedidos aguardando resposta (administradores)")
    public List<JoinRequestView> pending(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId) {
        return joinRequests.pending(groupId, me.id());
    }

    @PostMapping("/api/groups/{groupId}/join-requests/{requestId}/approve")
    @Operation(summary = "Aprova um pedido e coloca a pessoa no grupo")
    public ResponseEntity<Void> approve(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId, @PathVariable UUID requestId) {
        joinRequests.approve(groupId, requestId, me.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/groups/{groupId}/join-requests/{requestId}/reject")
    @Operation(summary = "Recusa um pedido")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId, @PathVariable UUID requestId) {
        joinRequests.reject(groupId, requestId, me.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/groups/{groupId}/join-requests/mine")
    @Operation(summary = "Cancela o próprio pedido de entrada")
    public ResponseEntity<Void> cancelMine(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId) {
        joinRequests.cancelOwn(groupId, me.id());
        return ResponseEntity.noContent().build();
    }
}
