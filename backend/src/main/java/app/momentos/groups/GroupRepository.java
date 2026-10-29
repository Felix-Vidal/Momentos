package app.momentos.groups;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    Optional<Group> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            select g from Group g
            where g.id in (select m.groupId from GroupMember m where m.userId = :userId)
            order by g.name
            """)
    List<Group> findAllOfMember(UUID userId);

    /**
     * Vitrine pública: só grupos abertos, buscando por nome sem diferenciar
     * acentos nem caixa (a função vem da migração V1).
     */
    @Query(value = """
            select * from groups g
            where g.visibility = 'OPEN'
              and (:termo = '' or momentos_slugify_search(g.name) like '%' || momentos_slugify_search(:termo) || '%')
            order by g.name
            """,
            countQuery = """
            select count(*) from groups g
            where g.visibility = 'OPEN'
              and (:termo = '' or momentos_slugify_search(g.name) like '%' || momentos_slugify_search(:termo) || '%')
            """,
            nativeQuery = true)
    Page<Group> searchOpen(String termo, Pageable pageable);
}
