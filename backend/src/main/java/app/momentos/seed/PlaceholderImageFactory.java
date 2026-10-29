package app.momentos.seed;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * Gera as imagens do seed: gradientes com o rótulo e a data, sem depender de rede.
 *
 * <p>Servem para a linha do tempo ter conteúdo de verdade — arquivos no
 * armazenamento, miniaturas, proporções variadas — até as fotos do grupo entrarem.
 */
@Component
public class PlaceholderImageFactory {

    private static final DateTimeFormatter LABEL_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", new Locale("pt", "BR"));

    /** Paleta alinhada ao tema da interface. */
    private static final Color[][] PALETTES = {
            {new Color(0x7C3AED), new Color(0xF5B53F)},
            {new Color(0xA855F7), new Color(0x34D399)},
            {new Color(0x1D1829), new Color(0xA855F7)},
            {new Color(0xF43F5E), new Color(0xF5B53F)},
            {new Color(0x0EA5E9), new Color(0xA855F7)},
    };

    public byte[] create(String label, Instant capturedAt, ZoneId zone, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color[] palette = PALETTES[Math.abs(label.hashCode()) % PALETTES.length];
        g.setPaint(new GradientPaint(0, 0, palette[0], width, height, palette[1]));
        g.fillRect(0, 0, width, height);

        // Círculos translúcidos, para a imagem não ficar um retângulo chapado.
        g.setColor(new Color(255, 255, 255, 26));
        g.fillOval(-width / 6, height / 3, width / 2, width / 2);
        g.fillOval(width / 2, -height / 4, width / 2, width / 2);

        int margin = Math.max(24, width / 20);
        g.setColor(new Color(12, 10, 20, 150));
        g.fillRoundRect(margin, height - margin - 104, width - margin * 2, 104, 24, 24);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(20, width / 26)));
        g.drawString(label, margin + 22, height - margin - 56);

        g.setColor(new Color(235, 230, 250, 220));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(14, width / 40)));
        g.drawString(LABEL_FORMAT.format(capturedAt.atZone(zone)), margin + 22, height - margin - 24);

        g.dispose();

        try {
            var output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao gerar imagem de demonstração", ex);
        }
    }
}
