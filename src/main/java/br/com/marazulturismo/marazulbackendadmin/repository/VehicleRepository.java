package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndIdNot(String licensePlate, Long id);

    List<Vehicle> findByStatus(VehicleStatus status);

    @Query("""
            select v.prefix from Vehicle v
            where v.status = :status
              and not exists (
                  select t.id from Trip t
                  where t.vehicle = v
                    and t.plannedDepartureOrigin < :periodEnd
                    and t.plannedArrivalOrigin > :periodStart)
            order by v.prefix asc
            """)
    List<String> findPrefixesWithoutTripBetween(@Param("status") VehicleStatus status,
                                                @Param("periodStart") LocalDateTime periodStart,
                                                @Param("periodEnd") LocalDateTime periodEnd);
}
