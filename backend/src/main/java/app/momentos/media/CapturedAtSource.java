package app.momentos.media;

/** De onde veio a data/hora de captura da mídia. */
public enum CapturedAtSource {
    /** Lida dos metadados EXIF do arquivo. */
    EXIF,
    /** Sem EXIF: usou-se o momento do envio. */
    UPLOAD,
    /** Ajustada à mão pelo dono da mídia. */
    MANUAL
}
