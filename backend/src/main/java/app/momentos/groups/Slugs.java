package app.momentos.groups;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** Geração de slugs legíveis a partir do nome do grupo. */
public final class Slugs {

    private Slugs() {
    }

    public static String from(String texto) {
        String base = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (base.isBlank()) {
            base = "grupo";
        }
        return base.length() > 60 ? base.substring(0, 60).replaceAll("-+$", "") : base;
    }

    /** Acrescenta sufixo numérico até o slug ficar livre. */
    public static String unique(String texto, Predicate<String> taken) {
        String base = from(texto);
        String candidate = base;
        int suffix = 2;
        while (taken.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
