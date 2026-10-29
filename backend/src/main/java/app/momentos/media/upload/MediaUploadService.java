package app.momentos.media.upload;

import app.momentos.common.config.MediaProperties;
import app.momentos.common.exception.ValidationException;
import app.momentos.media.CapturedAtSource;
import app.momentos.media.Media;
import app.momentos.media.MediaGroupShare;
import app.momentos.media.MediaGroupShareRepository;
import app.momentos.media.MediaRepository;
import app.momentos.media.MediaType;
import app.momentos.media.dto.MediaView;
import app.momentos.media.dto.UploadResult;
import app.momentos.media.processing.ExifCaptureDateExtractor;
import app.momentos.media.processing.FfmpegVideoProcessor;
import app.momentos.media.processing.ImageThumbnailGenerator;
import app.momentos.security.GroupAccessGuard;
import app.momentos.storage.StorageService;
import app.momentos.users.dto.UserSummary;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Recebe os arquivos enviados, descobre a data de captura, gera miniatura e grava
 * tudo no armazenamento.
 *
 * <p>Um arquivo recusado não derruba o lote: o resultado lista o que entrou e o que
 * ficou de fora, com o motivo.
 */
@Service
public class MediaUploadService {

    private static final Logger log = LoggerFactory.getLogger(MediaUploadService.class);
    private static final DateTimeFormatter FOLDER =
            DateTimeFormatter.ofPattern("yyyy/MM", Locale.ROOT);

    private final MediaRepository media;
    private final MediaGroupShareRepository shares;
    private final StorageService storage;
    private final GroupAccessGuard access;
    private final ExifCaptureDateExtractor exif;
    private final ImageThumbnailGenerator thumbnails;
    private final FfmpegVideoProcessor video;
    private final MediaProperties properties;

    public MediaUploadService(MediaRepository media, MediaGroupShareRepository shares,
            StorageService storage, GroupAccessGuard access, ExifCaptureDateExtractor exif,
            ImageThumbnailGenerator thumbnails, FfmpegVideoProcessor video,
            MediaProperties properties) {
        this.media = media;
        this.shares = shares;
        this.storage = storage;
        this.access = access;
        this.exif = exif;
        this.thumbnails = thumbnails;
        this.video = video;
        this.properties = properties;
    }

    @Transactional
    public UploadResult upload(List<MultipartFile> files, List<UUID> groupIds, UUID ownerId,
            UserSummary owner, ZoneId zone) {
        if (files == null || files.isEmpty()) {
            throw new ValidationException("Selecione ao menos um arquivo.");
        }
        // Compartilhar exige participar do grupo; a checagem vem antes de gravar nada.
        List<UUID> targets = groupIds == null ? List.of() : groupIds;
        targets.forEach(groupId -> access.requireMember(groupId, ownerId));

        List<MediaView> uploaded = new ArrayList<>();
        List<UploadResult.Rejected> rejected = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                Media saved = store(file, ownerId, zone);
                targets.forEach(groupId ->
                        shares.save(new MediaGroupShare(saved.getId(), groupId, ownerId)));
                uploaded.add(MediaView.of(saved, owner));
            } catch (ValidationException ex) {
                rejected.add(new UploadResult.Rejected(file.getOriginalFilename(), ex.getMessage()));
            } catch (IOException | RuntimeException ex) {
                log.warn("Falha ao processar {}: {}", file.getOriginalFilename(), ex.getMessage());
                rejected.add(new UploadResult.Rejected(file.getOriginalFilename(),
                        "Não foi possível processar este arquivo."));
            }
        }
        return new UploadResult(uploaded, rejected);
    }

    private Media store(MultipartFile file, UUID ownerId, ZoneId zone) throws IOException {
        MediaType type = typeOf(file.getContentType());
        checkSize(file, type);

        byte[] bytes = file.getBytes();
        Instant uploadedAt = Instant.now();
        UUID id = UUID.randomUUID();
        String extension = extensionOf(file.getOriginalFilename(), type);

        Media entity = type == MediaType.IMAGE
                ? storeImage(id, bytes, file, extension, ownerId, uploadedAt, zone)
                : storeVideo(id, bytes, file, extension, ownerId, uploadedAt);
        return media.save(entity);
    }

    private Media storeImage(UUID id, byte[] bytes, MultipartFile file, String extension,
            UUID ownerId, Instant uploadedAt, ZoneId zone) {
        // Sem EXIF, a data de envio serve de aproximação — e o dono pode corrigir depois.
        Instant captured = exif.extract(new ByteArrayInputStream(bytes), zone).orElse(uploadedAt);
        CapturedAtSource source = captured.equals(uploadedAt)
                ? CapturedAtSource.UPLOAD
                : CapturedAtSource.EXIF;

        String key = keyFor(id, captured, extension);
        storage.store(key, new ByteArrayInputStream(bytes), bytes.length, contentType(file));

        Media entity = new Media(ownerId, MediaType.IMAGE, key, contentType(file), bytes.length,
                captured, source);
        thumbnails.dimensions(bytes).ifPresent(dimensions -> {
            entity.setWidth(dimensions[0]);
            entity.setHeight(dimensions[1]);
        });
        thumbnails.generate(bytes).ifPresent(thumbnail -> {
            String thumbnailKey = thumbnailKeyFor(id, captured);
            storage.store(thumbnailKey, new ByteArrayInputStream(thumbnail), thumbnail.length,
                    "image/jpeg");
            entity.setThumbnailKey(thumbnailKey);
        });
        return entity;
    }

    private Media storeVideo(UUID id, byte[] bytes, MultipartFile file, String extension,
            UUID ownerId, Instant uploadedAt) {
        var processed = video.process(bytes, extension);

        byte[] finalBytes = processed.compressed().orElse(bytes);
        String finalExtension = processed.compressed().isPresent() ? ".mp4" : extension;
        String finalType = processed.compressed().isPresent() ? "video/mp4" : contentType(file);

        String key = keyFor(id, uploadedAt, finalExtension);
        storage.store(key, new ByteArrayInputStream(finalBytes), finalBytes.length, finalType);

        Media entity = new Media(ownerId, MediaType.VIDEO, key, finalType, finalBytes.length,
                uploadedAt, CapturedAtSource.UPLOAD);
        processed.duration().ifPresent(entity::setDurationSeconds);
        processed.width().ifPresent(entity::setWidth);
        processed.height().ifPresent(entity::setHeight);
        processed.thumbnail().ifPresent(thumbnail -> {
            String thumbnailKey = thumbnailKeyFor(id, uploadedAt);
            storage.store(thumbnailKey, new ByteArrayInputStream(thumbnail), thumbnail.length,
                    "image/jpeg");
            entity.setThumbnailKey(thumbnailKey);
        });
        return entity;
    }

    private void checkSize(MultipartFile file, MediaType type) {
        long limit = type == MediaType.IMAGE
                ? properties.maxImageSize().toBytes()
                : properties.maxVideoSize().toBytes();
        if (file.getSize() > limit) {
            throw new ValidationException("O arquivo passa do limite de "
                    + (limit / (1024 * 1024)) + " MB para "
                    + (type == MediaType.IMAGE ? "imagens" : "vídeos") + ".");
        }
        if (file.isEmpty()) {
            throw new ValidationException("O arquivo está vazio.");
        }
    }

    private static MediaType typeOf(String contentType) {
        if (contentType == null) {
            throw new ValidationException("Não foi possível identificar o tipo do arquivo.");
        }
        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }
        if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        throw new ValidationException("Só é possível enviar fotos e vídeos.");
    }

    private static String contentType(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    }

    private static String extensionOf(String fileName, MediaType type) {
        if (fileName != null) {
            int dot = fileName.lastIndexOf('.');
            if (dot > 0 && dot < fileName.length() - 1) {
                String extension = fileName.substring(dot).toLowerCase(Locale.ROOT);
                if (extension.matches("\\.[a-z0-9]{1,5}")) {
                    return extension;
                }
            }
        }
        return type == MediaType.IMAGE ? ".jpg" : ".mp4";
    }

    /** Chaves agrupadas por ano/mês, para a pasta de mídias não virar um diretório gigante. */
    private static String keyFor(UUID id, Instant captured, String extension) {
        return "media/" + ZonedDateTime.ofInstant(captured, ZoneId.of("UTC")).format(FOLDER)
                + "/" + id + extension;
    }

    private static String thumbnailKeyFor(UUID id, Instant captured) {
        return "thumbs/" + ZonedDateTime.ofInstant(captured, ZoneId.of("UTC")).format(FOLDER)
                + "/" + id + ".jpg";
    }
}
