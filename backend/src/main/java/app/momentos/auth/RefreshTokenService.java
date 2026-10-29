package app.momentos.auth;

import app.momentos.common.config.JwtProperties;
import app.momentos.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens opacos e rotativos: cada renovação revoga o token usado e emite outro,
 * de modo que um token roubado deixa de valer assim que o dono renova a sessão.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public String issue(UUID userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new RefreshToken(userId, hash(token),
                Instant.now().plus(properties.refreshTtl())));
        return token;
    }

    /** Valida o token, revoga-o e devolve o dono. */
    @Transactional
    public UUID rotate(String token) {
        RefreshToken stored = repository.findByTokenHash(hash(token))
                .orElseThrow(RefreshTokenService::invalid);
        if (!stored.isUsable(Instant.now())) {
            throw invalid();
        }
        stored.revoke();
        return stored.getUserId();
    }

    @Transactional
    public void revoke(String token) {
        repository.findByTokenHash(hash(token)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revokeAllOfUser(UUID userId) {
        repository.revokeAllOfUser(userId, Instant.now());
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "sessao_invalida",
                "Sua sessão expirou. Entre novamente.");
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", ex);
        }
    }
}
