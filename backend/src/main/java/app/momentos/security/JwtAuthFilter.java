package app.momentos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica a requisição pelo cabeçalho {@code Authorization: Bearer <token>} e, na
 * falta dele, pelo cookie de mídia.
 *
 * <p>O cookie carrega um token de escopo restrito: ele só autentica
 * <strong>GET de arquivo ou miniatura</strong>, que é o que a tag {@code <img>} precisa.
 * Qualquer outra rota ignora o cookie e volta a exigir o cabeçalho.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            jwtService.readAccessToken(header.substring(PREFIX.length()))
                    .ifPresent(JwtAuthFilter::authenticate);
        } else if (isMediaRead(request)) {
            String cookie = MediaAccessCookie.read(request);
            if (cookie != null) {
                jwtService.read(cookie)
                        .filter(payload -> JwtService.SCOPE_MEDIA.equals(payload.scope()))
                        .map(JwtService.TokenPayload::user)
                        .ifPresent(JwtAuthFilter::authenticate);
            }
        }
        chain.doFilter(request, response);
    }

    /** GET /api/media/{id}/file ou /thumbnail — o que o navegador carrega sozinho. */
    private static boolean isMediaRead(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return path.startsWith("/api/media/")
                && (path.endsWith("/file") || path.endsWith("/thumbnail"));
    }

    private static void authenticate(CurrentUser user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
