package app.momentos.categories.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Edição parcial: o que não vier fica como está. Por isso o nome não é obrigatório
 * aqui, ao contrário da criação — dá para trocar só a cor ou só o emoji.
 */
public record UpdateCategoryRequest(
        @Size(min = 2, max = 40, message = "O nome deve ter de 2 a 40 caracteres.")
        String name,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Escolha uma cor válida.")
        String color,

        @Size(max = 16, message = "Use apenas um emoji.")
        String emoji
) {
    public CategoryRequest asRequest() {
        return new CategoryRequest(name, color, emoji);
    }
}
