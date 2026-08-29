package br.com.marazulturismo.marazulbackendadmin.repository;

import java.util.List;
import java.util.Optional;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    List<User> findByPosition(Position position);

    List<User> findByDisabledAtIsNull();

    List<User> findByDisabledAtIsNotNull();

    List<User> findByPositionAndDisabledAtIsNull(Position position);

    List<User> findByPositionAndDisabledAtIsNotNull(Position position);

    @Query("SELECT COUNT(u) FROM User u WHERE u.disabledAt IS NULL")
    long countActiveEmployees();
}
