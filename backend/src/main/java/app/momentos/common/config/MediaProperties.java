package app.momentos.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Limites e parâmetros do processamento de mídia.
 *
 * @param maxImageSize   tamanho máximo por imagem
 * @param maxVideoSize   tamanho máximo por vídeo (configurável, como pedido)
 * @param thumbnailWidth largura da miniatura gerada
 * @param ffmpegPath     caminho do ffmpeg; "ffmpeg" procura no PATH
 * @param compressVideo  se false, o vídeo é guardado como veio mesmo havendo ffmpeg
 */
@ConfigurationProperties(prefix = "momentos.media")
public record MediaProperties(
        DataSize maxImageSize,
        DataSize maxVideoSize,
        Integer thumbnailWidth,
        String ffmpegPath,
        Boolean compressVideo
) {
    public MediaProperties {
        maxImageSize = maxImageSize == null ? DataSize.ofMegabytes(25) : maxImageSize;
        maxVideoSize = maxVideoSize == null ? DataSize.ofMegabytes(200) : maxVideoSize;
        thumbnailWidth = thumbnailWidth == null ? 640 : thumbnailWidth;
        ffmpegPath = ffmpegPath == null ? "ffmpeg" : ffmpegPath;
        compressVideo = compressVideo == null || compressVideo;
    }
}
