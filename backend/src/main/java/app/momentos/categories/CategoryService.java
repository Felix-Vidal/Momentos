package app.momentos.categories;

import app.momentos.categories.dto.CategoryRequest;
import app.momentos.categories.dto.CategoryView;
import app.momentos.common.exception.ConflictException;
import app.momentos.common.exception.ForbiddenException;
import app.momentos.common.exception.NotFoundException;
import app.momentos.media.MediaGroupShareRepository;
import app.momentos.security.GroupAccessGuard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CategoryService {

    /** Cor usada quando quem cria não escolhe nenhuma. */
    private static final String DEFAULT_COLOR = "#a855f7";

    private final CategoryRepository categories;
    private final MediaCategoryRepository marks;
    private final MediaGroupShareRepository shares;
    private final GroupAccessGuard access;

    public CategoryService(CategoryRepository categories, MediaCategoryRepository marks,
            MediaGroupShareRepository shares, GroupAccessGuard access) {
        this.categories = categories;
        this.marks = marks;
        this.shares = shares;
        this.access = access;
    }

    /** Qualquer membro cria categorias: o vocabulário do grupo é do grupo. */
    @Transactional
    public CategoryView create(UUID groupId, CategoryRequest request, UUID userId) {
        access.requireMember(groupId, userId);
        String name = request.name().trim();
        if (categories.existsByName(groupId, name, null)) {
            throw new ConflictException("Já existe uma categoria com esse nome neste grupo.");
        }
        Category category = categories.save(new Category(groupId, name,
                color(request.color()), emoji(request.emoji()), userId));
        return CategoryView.of(category, 0);
    }

    @Transactional(readOnly = true)
    public List<CategoryView> list(UUID groupId, UUID userId) {
        access.requireMember(groupId, userId);
        Map<UUID, Long> counts = counts(groupId);
        return categories.findByGroupIdOrderByName(groupId).stream()
                .map(category -> CategoryView.of(category,
                        counts.getOrDefault(category.getId(), 0L)))
                .toList();
    }

    @Transactional
    public CategoryView update(UUID categoryId, CategoryRequest request, UUID userId) {
        Category category = requireEditable(categoryId, userId);
        if (StringUtils.hasText(request.name())) {
            String name = request.name().trim();
            if (categories.existsByName(category.getGroupId(), name, categoryId)) {
                throw new ConflictException("Já existe uma categoria com esse nome neste grupo.");
            }
            category.setName(name);
        }
        if (StringUtils.hasText(request.color())) {
            category.setColor(request.color().toLowerCase());
        }
        if (request.emoji() != null) {
            category.setEmoji(emoji(request.emoji()));
        }
        return CategoryView.of(category,
                counts(category.getGroupId()).getOrDefault(categoryId, 0L));
    }

    @Transactional
    public void delete(UUID categoryId, UUID userId) {
        // As marcações saem junto por cascata; as mídias não são afetadas.
        categories.delete(requireEditable(categoryId, userId));
    }

    /**
     * Define as categorias de uma mídia dentro de um grupo. A lista substitui o que
     * havia, para a tela poder mandar o estado final das caixas marcadas.
     */
    @Transactional
    public List<CategoryView> setForMedia(UUID groupId, UUID mediaId, List<UUID> categoryIds,
            UUID userId) {
        access.requireMember(groupId, userId);
        if (!shares.existsByMediaIdAndGroupId(mediaId, groupId)) {
            throw NotFoundException.of("Mídia neste grupo");
        }

        Map<UUID, Category> doGrupo = categories.findByGroupIdOrderByName(groupId).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<UUID> desejadas = categoryIds.stream().distinct().toList();
        for (UUID id : desejadas) {
            if (!doGrupo.containsKey(id)) {
                // Categoria de outro grupo não pode vazar para esta marcação.
                throw NotFoundException.of("Categoria");
            }
        }

        List<MediaCategory> atuais = marks.findByMediaId(mediaId).stream()
                .filter(mark -> doGrupo.containsKey(mark.getCategoryId()))
                .toList();
        List<UUID> jaMarcadas = atuais.stream().map(MediaCategory::getCategoryId).toList();

        List<UUID> remover = jaMarcadas.stream().filter(id -> !desejadas.contains(id)).toList();
        if (!remover.isEmpty()) {
            marks.deleteByMediaIdAndCategoryIdIn(mediaId, remover);
        }
        desejadas.stream()
                .filter(id -> !jaMarcadas.contains(id))
                .forEach(id -> marks.save(new MediaCategory(mediaId, id, userId)));

        return desejadas.stream().map(id -> CategoryView.badge(doGrupo.get(id))).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryView> forMedia(UUID groupId, UUID mediaId, UUID userId) {
        access.requireMember(groupId, userId);
        return badgesFor(groupId, List.of(mediaId)).getOrDefault(mediaId, List.of());
    }

    /** Selos de várias mídias de uma vez, para a linha do tempo não fazer N+1. */
    @Transactional(readOnly = true)
    public Map<UUID, List<CategoryView>> badgesFor(UUID groupId, List<UUID> mediaIds) {
        if (mediaIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Category> doGrupo = categories.findByGroupIdOrderByName(groupId).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        Map<UUID, List<CategoryView>> result = new HashMap<>();
        for (MediaCategory mark : marks.findForMedia(groupId, mediaIds)) {
            Category category = doGrupo.get(mark.getCategoryId());
            if (category != null) {
                result.computeIfAbsent(mark.getMediaId(), key -> new ArrayList<>())
                        .add(CategoryView.badge(category));
            }
        }
        return result;
    }

    /** Ids das mídias marcadas com qualquer uma das categorias informadas. */
    @Transactional(readOnly = true)
    public List<UUID> mediaIdsIn(List<UUID> categoryIds) {
        return categoryIds.isEmpty() ? List.of() : marks.findMediaIdsByCategories(categoryIds);
    }

    private Map<UUID, Long> counts(UUID groupId) {
        return categories.countMediaByCategory(groupId).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> ((Number) row[1]).longValue()));
    }

    /** Quem criou a categoria pode editá-la; administradores do grupo também. */
    private Category requireEditable(UUID categoryId, UUID userId) {
        Category category = categories.findById(categoryId)
                .orElseThrow(() -> NotFoundException.of("Categoria"));
        var member = access.requireMember(category.getGroupId(), userId);
        if (!category.getCreatedBy().equals(userId) && !member.isAdmin()) {
            throw new ForbiddenException(
                    "Só quem criou a categoria ou um administrador pode alterá-la.");
        }
        return category;
    }

    private static String color(String value) {
        return StringUtils.hasText(value) ? value.toLowerCase() : DEFAULT_COLOR;
    }

    private static String emoji(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
