package app.momentos.media.processing;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lê a data/hora em que a foto foi tirada a partir do EXIF.
 *
 * <p>O EXIF normalmente guarda a hora local sem fuso. Interpretamos essa hora no fuso
 * informado (o do grupo/usuário), de modo que uma foto das 22:00 continue às 22:00 na
 * linha do tempo.
 */
@Component
public class ExifCaptureDateExtractor {

    private static final Logger log = LoggerFactory.getLogger(ExifCaptureDateExtractor.class);

    public Optional<Instant> extract(InputStream content, ZoneId zone) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(content);
            return originalDate(metadata, zone);
        } catch (ImageProcessingException | IOException | RuntimeException ex) {
            // Arquivo sem EXIF ou ilegível não é erro: o chamador usa a data de envio.
            log.debug("Sem data EXIF utilizável: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Instant> originalDate(Metadata metadata, ZoneId zone) {
        TimeZone timeZone = TimeZone.getTimeZone(zone);

        ExifSubIFDDirectory subIfd = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (subIfd != null) {
            Date original = subIfd.getDateOriginal(timeZone);
            if (original == null) {
                original = subIfd.getDateDigitized(timeZone);
            }
            if (original != null) {
                return Optional.of(original.toInstant());
            }
        }

        ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (ifd0 != null) {
            Date date = ifd0.getDate(ExifIFD0Directory.TAG_DATETIME, timeZone);
            if (date != null) {
                return Optional.of(date.toInstant());
            }
        }
        return Optional.empty();
    }
}
