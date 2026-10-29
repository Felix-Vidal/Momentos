package app.momentos.categories.dto;

import app.momentos.categories.Category;
import java.util.UUID;

/**
 * Categoria como a interface consome.
 *
 * @param mediaCount quantas mídias do grupo levam essa etiqueta
 */
public record CategoryView(
        UUID id,
        UUID groupId,
        String name,
        String color,
        String emoji,
        long mediaCount
) {
    public static CategoryView of(Category category, long mediaCount) {
        return new CategoryView(category.getId(), category.getGroupId(), category.getName(),
                category.getColor(), category.getEmoji(), mediaCount);
    }

    /** Versão enxuta, para o selo que acompanha cada mídia na linha do tempo. */
    public static CategoryView badge(Category category) {
        return of(category, 0);
    }
}
