package app.momentos.users;

import app.momentos.security.CurrentUser;
import app.momentos.users.dto.UpdateProfileRequest;
import app.momentos.users.dto.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
}
