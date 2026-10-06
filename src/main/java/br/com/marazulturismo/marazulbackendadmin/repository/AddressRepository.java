package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.Address;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @EntityGraph(attributePaths = {"city", "city.state"})
    List<Address> findAllByOrderByIdAsc();

    @Override
    @EntityGraph(attributePaths = {"city", "city.state"})
    Optional<Address> findById(Long id);
}
