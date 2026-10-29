package app.momentos.users;

import app.momentos.security.CurrentUser;
import app.momentos.media.MediaService;
import app.momentos.media.dto.MediaView;
import app.momentos.users.dto.UpdateProfileRequest;
import app.momentos.users.dto.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários")
public class UserController {

    private final UserService userService;
    private final MediaService mediaService;

    public UserController(UserService userService, MediaService mediaService) {
        this.userService = userService;
        this.mediaService = mediaService;
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil do usuário autenticado")
    public UserProfile me(@AuthenticationPrincipal CurrentUser me) {
        return UserProfile.own(userService.require(me.id()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Atualiza nome e bio do próprio perfil")
    public UserProfile updateMe(@AuthenticationPrincipal CurrentUser me,
            @Valid @RequestBody UpdateProfileRequest request) {
        return UserProfile.own(userService.updateProfile(me.id(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Perfil público de um usuário")
    public UserProfile byId(@PathVariable UUID id) {
        return UserProfile.publicView(userService.require(id));
    }

    @PutMapping("/me/avatar/{mediaId}")
    @Operation(summary = "Usa uma mídia própria como foto de perfil")
    public UserProfile setAvatar(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID mediaId) {
        return UserProfile.own(userService.setAvatar(me.id(), mediaId));
    }

    @DeleteMapping("/me/avatar")
    @Operation(summary = "Remove a foto de perfil")
    public UserProfile clearAvatar(@AuthenticationPrincipal CurrentUser me) {
        return UserProfile.own(userService.setAvatar(me.id(), null));
    }

    @GetMapping("/{id}/media")
    @Operation(summary = "Mídias que um integrante publicou em um grupo")
    public List<MediaView> mediaInGroup(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID id, @RequestParam UUID groupId,
            @RequestParam(defaultValue = "60") int limit) {
        return mediaService.byMemberInGroup(groupId, id, me.id(), Math.min(limit, 200));
    }
}
