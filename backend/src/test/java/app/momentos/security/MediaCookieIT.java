package app.momentos.security;

import static org.assertj.core.api.Assertions.assertThat;

import app.momentos.media.processing.ExifFixtures;
import app.momentos.support.ApiTest;
import app.momentos.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Uma tag {@code <img>} não manda cabeçalho de autenticação: o navegador faz a
 * requisição sozinho. Estes testes garantem que o cookie resolve exatamente esse
 * caso — e só ele.
 */
@IntegrationTest
class MediaCookieIT extends ApiTest {

    private String cookie;
    private Session dono;
    private String mediaId;

    @BeforeEach
    void cenario() throws Exception {
        cleanDatabase();

        // O login devolve o cookie de mídia junto da sessão.
        var response = api().post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterPayload("Felix", "felix@ufac.br", "senhaSegura123"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SessionPayload.class)
                .returnResult();

        var body = response.getResponseBody();
        dono = new Session(body.accessToken(), body.refreshToken(),
                UUID.fromString(body.user().id()));
        cookie = response.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);

        UUID grupoId = createGroup(dono, "Sociedade da Roleta", "OPEN");
        var builder = new MultipartBodyBuilder();
        builder.part("files", new ByteArrayResource(ExifFixtures.jpegWithoutExif(200, 150)))
                .filename("foto.jpg").contentType(MediaType.IMAGE_JPEG);
        mediaId = api(dono.accessToken()).post()
                .uri("/api/media?groupIds=" + grupoId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .exchange().expectStatus().isOk()
                .expectBody(UploadPayload.class).returnResult().getResponseBody()
                .uploaded().getFirst().id();
    }

    @Test
    void o_login_instala_um_cookie_httponly_restrito_a_midia() {
        assertThat(cookie).isNotNull();
        assertThat(cookie).contains("momentos_media=");
        // As três proteções que tornam o cookie aceitável: o JS não lê, não viaja
        // para outros sites e vale só no caminho das mídias.
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("SameSite=Lax");
        assertThat(cookie).contains("Path=/api/media");
    }

    @Test
    void com_o_cookie_a_imagem_carrega_sem_cabecalho_de_autenticacao() {
        browser().get().uri("/api/media/" + mediaId + "/thumbnail")
                .exchange().expectStatus().isOk();

        browser().get().uri("/api/media/" + mediaId + "/file")
                .exchange().expectStatus().isOk();
    }

    @Test
    void sem_o_cookie_e_sem_cabecalho_a_midia_continua_protegida() {
        api().get().uri("/api/media/" + mediaId + "/thumbnail")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void o_cookie_nao_serve_para_o_resto_da_api() {
        // Mesmo com o cookie, ler dados ou agir exige o cabeçalho Authorization.
        browser().get().uri("/api/users/me").exchange().expectStatus().isUnauthorized();
        browser().get().uri("/api/media/mine").exchange().expectStatus().isUnauthorized();
        browser().delete().uri("/api/media/" + mediaId).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void o_cookie_de_outra_pessoa_nao_abre_midia_sem_acesso() {
        // Uma segunda conta, em grupo próprio, não alcança a mídia do primeiro.
        var outra = api().post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterPayload("Ana", "ana@ufac.br", "senhaSegura123"))
                .exchange().expectStatus().isCreated()
                .expectBody(SessionPayload.class).returnResult();
        String cookieDaAna = outra.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);

        RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.COOKIE, cookieDaAna)
                .build()
                .get().uri("/api/media/" + mediaId + "/thumbnail")
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void o_logout_apaga_o_cookie() {
        var headers = api().post().uri("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshPayload(dono.refreshToken()))
                .exchange().expectStatus().isNoContent()
                .expectBody().returnResult().getResponseHeaders();

        assertThat(headers.getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    /** Cliente que imita o navegador: manda o cookie, nunca o cabeçalho. */
    private RestTestClient browser() {
        return RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.COOKIE, cookie)
                .build();
    }

    record RefreshPayload(String refreshToken) {
    }

    record UploadPayload(java.util.List<IdPayload> uploaded) {
    }

    record IdPayload(String id) {
    }
}
