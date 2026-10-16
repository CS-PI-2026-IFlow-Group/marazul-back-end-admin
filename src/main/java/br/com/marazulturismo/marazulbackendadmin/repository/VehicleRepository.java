package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndIdNot(String licensePlate, Long id);

    List<Vehicle> findByStatus(VehicleStatus status);
}
