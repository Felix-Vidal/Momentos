package app.momentos.auth;

import app.momentos.auth.dto.AuthResponse;
import app.momentos.auth.dto.LoginRequest;
import app.momentos.auth.dto.RefreshRequest;
import app.momentos.auth.dto.RegisterRequest;
import app.momentos.security.JwtService;
import app.momentos.security.MediaAccessCookie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final MediaAccessCookie mediaCookie;

    public AuthController(AuthService authService, JwtService jwtService,
            MediaAccessCookie mediaCookie) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.mediaCookie = mediaCookie;
    }

    @PostMapping("/register")
    @Operation(summary = "Cria uma conta e já devolve a sessão")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest http) {
        return withMediaCookie(authService.register(request), http, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Entra com e-mail e senha")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest http) {
        return withMediaCookie(authService.login(request), http, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Troca o refresh token por uma nova sessão (o token usado é revogado)")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request,
            HttpServletRequest http) {
        return withMediaCookie(authService.refresh(request.refreshToken()), http, HttpStatus.OK);
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerra a sessão revogando o refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request,
            HttpServletRequest http) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, mediaCookie.clear(http.isSecure()))
                .build();
    }

    /**
     * Toda resposta que abre sessão instala também o cookie de mídia, para o navegador
     * conseguir carregar fotos e vídeos em tags {@code <img>} e {@code <video>} — que
     * não mandam cabeçalho de autenticação.
     */
    private ResponseEntity<AuthResponse> withMediaCookie(AuthResponse body,
            HttpServletRequest http, HttpStatus status) {
        String token = jwtService.issueMediaToken(
                body.user().id(), body.user().email(), body.user().name());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE,
                        mediaCookie.set(token, jwtService.mediaTokenSeconds(), http.isSecure()))
                .body(body);
    }
}
