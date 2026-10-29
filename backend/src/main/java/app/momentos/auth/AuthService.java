package app.momentos.auth;

import app.momentos.auth.dto.AuthResponse;
import app.momentos.auth.dto.LoginRequest;
import app.momentos.auth.dto.RegisterRequest;
import app.momentos.common.exception.ApiException;
import app.momentos.common.exception.ConflictException;
import app.momentos.security.JwtService;
import app.momentos.users.User;
import app.momentos.users.UserRepository;
import app.momentos.users.dto.UserProfile;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
            JwtService jwtService, RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim();
        if (users.existsByEmail(email)) {
            throw new ConflictException("Já existe uma conta com esse e-mail.");
        }
        User user = users.save(new User(
                request.name().trim(), email, passwordEncoder.encode(request.password())));
        return respond(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(request.email().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "credenciais_invalidas",
                        "E-mail ou senha incorretos."));
        return respond(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        UUID userId = refreshTokens.rotate(refreshToken);
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "sessao_invalida",
                        "Sua sessão expirou. Entre novamente."));
        return respond(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    private AuthResponse respond(User user) {
        return new AuthResponse(
                jwtService.issueAccessToken(user),
                refreshTokens.issue(user.getId()),
                jwtService.accessTokenSeconds(),
                UserProfile.own(user));
    }
}
