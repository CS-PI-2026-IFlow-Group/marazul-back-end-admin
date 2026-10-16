package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Override
    @EntityGraph(attributePaths = "permissions")
    List<Profile> findAll();

    @Override
    @EntityGraph(attributePaths = "permissions")
    Optional<Profile> findById(Long id);
}
