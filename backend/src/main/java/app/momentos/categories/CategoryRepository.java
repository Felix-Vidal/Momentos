package app.momentos.categories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByGroupIdOrderByName(UUID groupId);

    Optional<Category> findByIdAndGroupId(UUID id, UUID groupId);

    /** Quantas mídias cada categoria do grupo tem, para o resumo e os filtros. */
    @Query(value = """
            select c.id, count(mc.media_id)
            from categories c
            left join media_categories mc on mc.category_id = c.id
            where c.group_id = :groupId
            group by c.id
            """, nativeQuery = true)
    List<Object[]> countMediaByCategory(UUID groupId);

    @Query(value = """
            select exists (
                select 1 from categories
                where group_id = :groupId
                  and momentos_slugify_search(name) = momentos_slugify_search(:name)
                  and (:excluding is null or id <> :excluding)
            )
            """, nativeQuery = true)
    boolean existsByName(UUID groupId, String name, UUID excluding);
}
