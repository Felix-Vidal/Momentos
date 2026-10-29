package app.momentos.categories.dto;

import java.util.List;
import java.util.UUID;

/**
 * Define as categorias de uma mídia dentro de um grupo. A lista substitui o que
 * havia: o que não vier nela é removido.
 */
public record SetCategoriesRequest(List<UUID> categoryIds) {

    public SetCategoriesRequest {
        categoryIds = categoryIds == null ? List.of() : categoryIds;
    }
}
