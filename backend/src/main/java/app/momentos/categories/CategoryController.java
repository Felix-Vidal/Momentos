package app.momentos.categories;

import app.momentos.categories.dto.CategoryRequest;
import app.momentos.categories.dto.CategoryView;
import app.momentos.categories.dto.SetCategoriesRequest;
import app.momentos.categories.dto.UpdateCategoryRequest;
import app.momentos.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Categorias")
public class CategoryController {

    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping("/api/groups/{groupId}/categories")
    @Operation(summary = "Categorias do grupo, com a contagem de mídias de cada uma")
    public List<CategoryView> list(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId) {
        return categories.list(groupId, me.id());
    }

    @PostMapping("/api/groups/{groupId}/categories")
    @Operation(summary = "Cria uma categoria do grupo (qualquer membro)")
    public ResponseEntity<CategoryView> create(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID groupId, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categories.create(groupId, request, me.id()));
    }

    @PatchMapping("/api/categories/{categoryId}")
    @Operation(summary = "Edita nome, cor ou emoji (autor ou administrador)")
    public CategoryView update(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID categoryId, @Valid @RequestBody UpdateCategoryRequest request) {
        return categories.update(categoryId, request.asRequest(), me.id());
    }

    @DeleteMapping("/api/categories/{categoryId}")
    @Operation(summary = "Apaga a categoria; as mídias continuam, apenas sem a etiqueta")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID categoryId) {
        categories.delete(categoryId, me.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/media/{mediaId}/categories")
    @Operation(summary = "Categorias de uma mídia dentro de um grupo")
    public List<CategoryView> ofMedia(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID mediaId, @RequestParam UUID groupId) {
        return categories.forMedia(groupId, mediaId, me.id());
    }

    @PutMapping("/api/media/{mediaId}/categories")
    @Operation(summary = "Define as categorias da mídia naquele grupo (a lista substitui)")
    public List<CategoryView> setForMedia(@AuthenticationPrincipal CurrentUser me,
            @PathVariable UUID mediaId, @RequestParam UUID groupId,
            @Valid @RequestBody SetCategoriesRequest request) {
        return categories.setForMedia(groupId, mediaId, request.categoryIds(), me.id());
    }
}
