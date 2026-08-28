package br.com.marazulturismo.marazulbackendadmin.repository;

import java.util.Optional;

import br.com.marazulturismo.marazulbackendadmin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    @Query("SELECT COUNT(u) FROM User u WHERE u.disabledAt IS NULL")
    long countActiveEmployees();
}
