package app.momentos.security;

import app.momentos.common.config.JwtProperties;
import app.momentos.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Emissão e leitura dos access tokens (JWT assinado com HMAC-SHA256). */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_SCOPE = "scope";

    /** Token normal da API: vale para tudo que o usuário pode fazer. */
    public static final String SCOPE_ACCESS = "access";
    /**
     * Token que viaja em cookie para a tag {@code <img>}/{@code <video>} poder carregar
     * arquivos. Vale somente para ler mídia — ver {@link JwtAuthFilter}.
     */
    public static final String SCOPE_MEDIA = "media";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = resolveKey(properties.secret());
    }

    public String issueAccessToken(User user) {
        return issue(user.getId(), user.getEmail(), user.getName(), SCOPE_ACCESS,
                properties.accessTtl());
    }

    /**
     * Token de leitura de mídia, guardado em cookie httpOnly. Vive tanto quanto a
     * sessão porque o navegador não tem como renovar o cookie no meio de um
     * carregamento de imagem.
     */
    public String issueMediaToken(UUID userId, String email, String name) {
        return issue(userId, email, name, SCOPE_MEDIA, properties.refreshTtl());
    }

    /** Devolve o usuário do token de API, ou vazio se não servir. */
    public Optional<CurrentUser> readAccessToken(String token) {
        return read(token)
                .filter(payload -> SCOPE_ACCESS.equals(payload.scope()))
                .map(TokenPayload::user);
    }

    /** Lê qualquer token válido, informando o escopo. */
    public Optional<TokenPayload> read(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String scope = claims.get(CLAIM_SCOPE, String.class);
            return Optional.of(new TokenPayload(
                    new CurrentUser(
                            UUID.fromString(claims.getSubject()),
                            claims.get(CLAIM_EMAIL, String.class),
                            claims.get(CLAIM_NAME, String.class)),
                    scope == null ? SCOPE_ACCESS : scope));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long mediaTokenSeconds() {
        return properties.refreshTtl().toSeconds();
    }

    private String issue(UUID userId, String email, String name, String scope,
            java.time.Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_NAME, name)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_SCOPE, scope)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Conteúdo de um token lido. */
    public record TokenPayload(CurrentUser user, String scope) {
    }

    public long accessTokenSeconds() {
        return properties.accessTtl().toSeconds();
    }

    private static SecretKey resolveKey(String secret) {
        if (StringUtils.hasText(secret)) {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
        log.warn("momentos.jwt.secret não definido: gerando chave temporária. "
                + "Os tokens deixam de valer ao reiniciar a aplicação. "
                + "Defina MOMENTOS_JWT_SECRET em produção.");
        return Jwts.SIG.HS256.key().build();
    }
}
