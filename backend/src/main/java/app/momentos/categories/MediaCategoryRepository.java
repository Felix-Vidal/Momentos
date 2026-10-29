package app.momentos.categories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MediaCategoryRepository extends JpaRepository<MediaCategory, UUID> {

    List<MediaCategory> findByMediaId(UUID mediaId);

    @Modifying
    @Query("delete from MediaCategory mc where mc.mediaId = :mediaId and mc.categoryId in :ids")
    void deleteByMediaIdAndCategoryIdIn(UUID mediaId, List<UUID> ids);

    /** Marcações das mídias de uma página da linha do tempo, em uma consulta só. */
    @Query("""
            select mc from MediaCategory mc
            where mc.mediaId in :mediaIds
              and mc.categoryId in (select c.id from Category c where c.groupId = :groupId)
            """)
    List<MediaCategory> findForMedia(UUID groupId, List<UUID> mediaIds);

    /** Ids das mídias que têm ao menos uma das categorias — usado pelo filtro. */
    @Query("select distinct mc.mediaId from MediaCategory mc where mc.categoryId in :categoryIds")
    List<UUID> findMediaIdsByCategories(List<UUID> categoryIds);
}
