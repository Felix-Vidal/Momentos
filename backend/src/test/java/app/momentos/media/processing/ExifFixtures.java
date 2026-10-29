package app.momentos.media.processing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;

/**
 * Gera JPEGs de teste, com e sem EXIF.
 *
 * <p>O ImageIO não escreve EXIF, então o segmento APP1 é montado à mão: cabeçalho
 * TIFF big-endian, IFD0 apontando para o ExifIFD e uma única entrada
 * DateTimeOriginal (0x9003).
 */
public final class ExifFixtures {

    private static final DateTimeFormatter EXIF_FORMAT =
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    private ExifFixtures() {
    }

    public static byte[] jpegWithoutExif(int width, int height) throws IOException {
        var output = new ByteArrayOutputStream();
        ImageIO.write(sampleImage(width, height), "jpg", output);
        return output.toByteArray();
    }

    public static byte[] jpegWithCaptureDate(LocalDateTime capturedAt) throws IOException {
        byte[] plain = jpegWithoutExif(200, 120);
        byte[] exif = exifApp1(capturedAt);

        var result = new ByteArrayOutputStream();
        result.write(plain, 0, 2);                 // SOI
        result.write(exif);                        // APP1 com o EXIF
        result.write(plain, 2, plain.length - 2);  // restante do JPEG
        return result.toByteArray();
    }

    private static BufferedImage sampleImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x7C, 0x3A, 0xED),
                width, height, new Color(0xF5, 0xB5, 0x3F)));
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private static byte[] exifApp1(LocalDateTime capturedAt) {
        byte[] date = (capturedAt.format(EXIF_FORMAT) + "\0").getBytes(StandardCharsets.US_ASCII);

        // TIFF: header (8) + IFD0 (18) + ExifIFD (18) + string da data
        ByteBuffer tiff = ByteBuffer.allocate(44 + date.length);
        tiff.put((byte) 'M').put((byte) 'M');  // big-endian
        tiff.putShort((short) 42);
        tiff.putInt(8);                        // offset do IFD0

        tiff.putShort((short) 1);              // IFD0: uma entrada
        tiff.putShort((short) 0x8769);         // ExifIFDPointer
        tiff.putShort((short) 4);              // LONG
        tiff.putInt(1);
        tiff.putInt(26);                       // onde começa o ExifIFD
        tiff.putInt(0);                        // não há IFD1

        tiff.putShort((short) 1);              // ExifIFD: uma entrada
        tiff.putShort((short) 0x9003);         // DateTimeOriginal
        tiff.putShort((short) 2);              // ASCII
        tiff.putInt(date.length);
        tiff.putInt(44);                       // onde está a string
        tiff.putInt(0);

        tiff.put(date);

        byte[] tiffBytes = tiff.array();
        byte[] prefix = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);
        int length = 2 + prefix.length + tiffBytes.length;

        ByteBuffer app1 = ByteBuffer.allocate(2 + length);
        app1.put((byte) 0xFF).put((byte) 0xE1);
        app1.putShort((short) length);
        app1.put(prefix);
        app1.put(tiffBytes);
        return app1.array();
    }
}
