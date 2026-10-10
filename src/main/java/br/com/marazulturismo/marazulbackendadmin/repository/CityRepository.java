package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByNameIgnoreCaseAndStateAcronymIgnoreCase(String name, String acronym);

    List<City> findByStateIdOrderByNameAsc(Long stateId);

    List<City> findByStateAcronymIgnoreCaseOrderByNameAsc(String acronym);

    List<City> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<City> findByStateIdAndNameContainingIgnoreCaseOrderByNameAsc(Long stateId, String name);
}
