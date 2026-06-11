package br.com.marazulturismo.marazulbackendadmin.repository;

import java.util.Optional;

import br.com.marazulturismo.marazulbackendadmin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);
}
