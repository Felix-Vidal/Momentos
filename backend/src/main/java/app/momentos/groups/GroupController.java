package app.momentos.groups;

import app.momentos.common.web.PageResponse;
import app.momentos.groups.dto.ChangeRoleRequest;
import app.momentos.groups.dto.CreateGroupRequest;
import app.momentos.groups.dto.GroupDetail;
import app.momentos.groups.dto.GroupMemberView;
import app.momentos.groups.dto.GroupSummary;
import app.momentos.groups.dto.PublicGroupCard;
import app.momentos.groups.dto.UpdateGroupRequest;
import app.momentos.invites.JoinRequestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import app.momentos.security.CurrentUser;

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Grupos")
public class GroupController {

    private final GroupService groupService;
    private final JoinRequestRepository joinRequests;

    public GroupController(GroupService groupService, JoinRequestRepository joinRequests) {
        this.groupService = groupService;
        this.joinRequests = joinRequests;
    }

    @PostMapping
    @Operation(summary = "Cria um grupo; quem cria vira administrador")
    public ResponseEntity<GroupDetail> create(@AuthenticationPrincipal CurrentUser me,
            @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.create(request, me.id()));
    }

    @GetMapping
    @Operation(summary = "Grupos dos quais participo")
    public List<GroupSummary> mine(@AuthenticationPrincipal CurrentUser me) {
        return groupService.listMine(me.id());
    }

    @GetMapping("/public")
    @Operation(summary = "Vitrine de grupos abertos (sem conteúdo, acessível a qualquer pessoa)")
    public PageResponse<PublicGroupCard> discover(
            @AuthenticationPrincipal CurrentUser me,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        UUID viewer = me == null ? null : me.id();
        return PageResponse.of(groupService.discover(q, PageRequest.of(page, Math.min(size, 50)),
                viewer, joinRequests::hasPending));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Detalhe do grupo (só membros)")
    public GroupDetail detail(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId) {
        return groupService.detail(groupId, me.id());
    }

    @GetMapping("/by-slug/{slug}")
    @Operation(summary = "Detalhe do grupo pelo slug (só membros)")
    public GroupDetail bySlug(@AuthenticationPrincipal CurrentUser me, @PathVariable String slug) {
        return groupService.detail(groupService.requireBySlug(slug).getId(), me.id());
    }

    @PatchMapping("/{groupId}")
    @Operation(summary = "Atualiza nome, descrição, visibilidade ou tema (administradores)")
    public GroupDetail update(@AuthenticationPrincipal CurrentUser me, @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest request) {
        return groupService.update(groupId, request, me.id());
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "Integrantes do grupo (só membros)")
    public List<GroupMemberView> members(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId) {
        return groupService.listMembers(groupId, me.id());
    }

    @PatchMapping("/{groupId}/members/{userId}")
    @Operation(summary = "Promove ou rebaixa um membro (administradores)")
    public GroupMemberView changeRole(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId, @PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        return groupService.changeRole(groupId, userId, request.role(), me.id());
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @Operation(summary = "Remove um membro; o próprio usuário usa isso para sair do grupo")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId, @PathVariable UUID userId) {
        groupService.removeMember(groupId, userId, me.id());
        return ResponseEntity.noContent().build();
    }
}
