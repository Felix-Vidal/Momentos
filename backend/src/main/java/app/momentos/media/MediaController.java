package app.momentos.media;

import app.momentos.media.dto.MediaView;
import app.momentos.media.dto.ShareRequest;
import app.momentos.media.dto.UpdateMediaRequest;
import app.momentos.media.dto.UploadResult;
import app.momentos.media.upload.MediaUploadService;
import app.momentos.security.CurrentUser;
import app.momentos.storage.StoredObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Mídias")
public class MediaController {

    private final MediaService mediaService;
    private final MediaUploadService uploadService;

    public MediaController(MediaService mediaService, MediaUploadService uploadService) {
        this.mediaService = mediaService;
        this.uploadService = uploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia uma ou várias mídias e já compartilha com os grupos escolhidos")
    public UploadResult upload(
            @AuthenticationPrincipal CurrentUser me,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(name = "groupIds", required = false) List<UUID> groupIds,
            @RequestParam(name = "timeZone", required = false) String timeZone) {
        return uploadService.upload(files, groupIds, me.id(),
                mediaService.summaryOf(me.id()), zoneOf(timeZone));
    }

    @GetMapping("/mine")
    @Operation(summary = "Minhas mídias, das mais recentes para as mais antigas")
    public List<MediaView> mine(@AuthenticationPrincipal CurrentUser me,
            @RequestParam(defaultValue = "60") int limit) {
        return mediaService.mine(me.id(), Math.min(limit, 200));
    }

    @GetMapping("/{mediaId}/file")
    @Operation(summary = "Arquivo original (só para quem tem acesso à mídia)")
    public ResponseEntity<InputStreamResource> file(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID mediaId) {
        return stream(mediaService.openOriginal(mediaId, me.id()));
    }

    @GetMapping("/{mediaId}/thumbnail")
    @Operation(summary = "Miniatura da mídia")
    public ResponseEntity<InputStreamResource> thumbnail(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID mediaId) {
        return stream(mediaService.openThumbnail(mediaId, me.id()));
    }

    @PatchMapping("/{mediaId}")
    @Operation(summary = "Corrige a data de captura ou a legenda (só o dono)")
    public MediaView update(@AuthenticationPrincipal CurrentUser me, @PathVariable UUID mediaId,
            @Valid @RequestBody UpdateMediaRequest request) {
        return mediaService.update(mediaId, request, me.id());
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Apaga a mídia e seus arquivos (só o dono)")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID mediaId) {
        mediaService.delete(mediaId, me.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{mediaId}/groups")
    @Operation(summary = "Grupos em que a mídia está compartilhada (só o dono)")
    public List<UUID> groups(@AuthenticationPrincipal CurrentUser me, @PathVariable UUID mediaId) {
        return mediaService.groupsOf(mediaId, me.id());
    }

    @PostMapping("/{mediaId}/groups")
    @Operation(summary = "Compartilha a mídia com grupos dos quais participo")
    public ResponseEntity<Void> share(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID mediaId, @Valid @RequestBody ShareRequest request) {
        mediaService.share(mediaId, request.groupIds(), me.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{mediaId}/groups/{groupId}")
    @Operation(summary = "Retira a mídia de um grupo (o dono ou um administrador)")
    public ResponseEntity<Void> unshare(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID mediaId, @PathVariable UUID groupId) {
        mediaService.unshare(mediaId, groupId, me.id());
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<InputStreamResource> stream(StoredObject object) {
        var builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(object.contentType()))
                // Mídia é imutável: pode ficar em cache por bastante tempo.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes");
        if (object.sizeBytes() >= 0) {
            builder.contentLength(object.sizeBytes());
        }
        return builder.body(new InputStreamResource(object.stream()));
    }

    private static ZoneId zoneOf(String timeZone) {
        try {
            return timeZone == null || timeZone.isBlank()
                    ? ZoneId.systemDefault()
                    : ZoneId.of(timeZone);
        } catch (RuntimeException ex) {
            return ZoneId.systemDefault();
        }
    }
}
