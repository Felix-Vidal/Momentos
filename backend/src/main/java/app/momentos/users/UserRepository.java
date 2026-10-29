package app.momentos.users;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmail(String email);

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(:email)")
    boolean existsByEmail(String email);

    @Query("""
            select u from User u
            where lower(u.name) like lower(concat('%', :termo, '%'))
               or lower(u.email) = lower(:termo)
            order by u.name
            """)
    List<User> search(String termo);
}
