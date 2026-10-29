package app.momentos.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

/**
 * Cookie que permite ao navegador carregar fotos e vídeos.
 *
 * <p>Uma tag {@code <img>} ou {@code <video>} não manda cabeçalho {@code Authorization}:
 * o navegador faz a requisição sozinho. Sem este cookie, toda mídia responderia 401 e a
 * linha do tempo apareceria vazia.
 *
 * <p>Por isso o cookie é restrito ao máximo: {@code HttpOnly} (JavaScript não lê),
 * {@code SameSite=Lax} (não viaja em requisição de outro site), {@code Path=/api/media}
 * e com um token cujo escopo só serve para <strong>ler</strong> arquivos de mídia.
 */
@Component
public class MediaAccessCookie {

    public static final String NAME = "momentos_media";

    /** Cabeçalho `Set-Cookie` que instala o cookie. */
    public String set(String token, long maxAgeSeconds, boolean secure) {
        return ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/api/media")
                .maxAge(maxAgeSeconds)
                .build()
                .toString();
    }

    /** Cabeçalho que remove o cookie, usado no logout. */
    public String clear(boolean secure) {
        return set("", 0, secure);
    }

    public void applyTo(MultiValueMap<String, String> headers, String token,
            long maxAgeSeconds, HttpServletRequest request) {
        headers.add(HttpHeaders.SET_COOKIE, set(token, maxAgeSeconds, request.isSecure()));
    }

    public static String read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var cookie : request.getCookies()) {
            if (NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
