package app.momentos.media.dto;

import java.util.List;

/**
 * Resultado de um envio em lote: o que entrou e o que foi recusado, para a interface
 * mostrar o erro por arquivo sem perder o lote inteiro.
 */
public record UploadResult(List<MediaView> uploaded, List<Rejected> rejected) {

    public record Rejected(String fileName, String reason) {
    }
}
