package app.momentos.media.processing;

import app.momentos.common.config.MediaProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Miniaturas de imagem em JPEG, para a linha do tempo carregar rápido. */
@Component
public class ImageThumbnailGenerator {

    private static final Logger log = LoggerFactory.getLogger(ImageThumbnailGenerator.class);

    private final MediaProperties properties;

    public ImageThumbnailGenerator(MediaProperties properties) {
        this.properties = properties;
    }

    /** Dimensões originais da imagem, quando legíveis. */
    public Optional<int[]> dimensions(byte[] original) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
            return image == null
                    ? Optional.empty()
                    : Optional.of(new int[] {image.getWidth(), image.getHeight()});
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    public Optional<byte[]> generate(byte[] original) {
        try {
            var output = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(original))
                    .size(properties.thumbnailWidth(), properties.thumbnailWidth())
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.82)
                    .toOutputStream(output);
            return Optional.of(output.toByteArray());
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException ex) {
            log.warn("Não foi possível gerar miniatura: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
