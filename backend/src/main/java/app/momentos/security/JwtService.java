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

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = resolveKey(properties.secret());
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_NAME, user.getName())
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTtl())))
                .signWith(key)
                .compact();
    }

    /** Devolve o usuário do token, ou vazio se o token for inválido ou expirado. */
    public Optional<CurrentUser> readAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new CurrentUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    claims.get(CLAIM_NAME, String.class)));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
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
