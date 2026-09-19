package br.com.marazulturismo.marazulbackendadmin.repository;

import java.util.List;
import java.util.Optional;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CollaboratorRepository extends JpaRepository<Collaborator, Long> {

    boolean existsByEmail(String email);

    Optional<Collaborator> findByEmail(String email);

    Optional<Collaborator> findByResetToken(String resetToken);

    List<Collaborator> findByPosition(Position position);

    List<Collaborator> findByDisabledAtIsNull();

    List<Collaborator> findByDisabledAtIsNotNull();

    List<Collaborator> findByPositionAndDisabledAtIsNull(Position position);

    List<Collaborator> findByPositionAndDisabledAtIsNotNull(Position position);

    @Query("SELECT COUNT(c) FROM Collaborator c WHERE c.disabledAt IS NULL")
    long countActiveEmployees();
}
