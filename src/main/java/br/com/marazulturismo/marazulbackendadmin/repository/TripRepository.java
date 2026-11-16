package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Override
    @EntityGraph(attributePaths = {"originCity", "destinationCity", "vehicle", "drivers"})
    List<Trip> findAll();

    @Override
    @EntityGraph(attributePaths = {"originCity", "destinationCity", "vehicle", "drivers"})
    Optional<Trip> findById(Long id);

    @EntityGraph(attributePaths = {
            "originCity", "originCity.state", "destinationCity", "destinationCity.state", "vehicle", "client"})
    @Query("""
            select t from Trip t
            where (:status is null or t.status = :status)
              and (:periodStart is null or t.plannedDepartureOrigin >= :periodStart)
              and (:periodEnd is null or t.plannedDepartureOrigin < :periodEnd)
              and (:cityId is null or t.originCity.id = :cityId or t.destinationCity.id = :cityId)
              and (:vehicleId is null or t.vehicle.id = :vehicleId)
            order by t.plannedDepartureOrigin asc
            """)
    List<Trip> findByFilters(@Param("status") br.com.marazulturismo.marazulbackendadmin.enums.TripStatus status,
                             @Param("periodStart") LocalDateTime periodStart,
                             @Param("periodEnd") LocalDateTime periodEnd,
                             @Param("cityId") Long cityId,
                             @Param("vehicleId") Long vehicleId);

    @Query("select (count(t) > 0) from Trip t where t.vehicle.id = :vehicleId " +
            "and (:excludeTripId is null or t.id != :excludeTripId) " +
            "and t.plannedDepartureOrigin < :periodEnd and t.plannedArrivalOrigin > :periodStart")
    boolean existsVehicleOverlap(@Param("vehicleId") Long vehicleId,
                                 @Param("periodStart") LocalDateTime periodStart,
                                 @Param("periodEnd") LocalDateTime periodEnd,
                                 @Param("excludeTripId") Long excludeTripId);

    @Query("select (count(t) > 0) from Trip t join t.drivers d where d.id = :driverId " +
            "and (:excludeTripId is null or t.id != :excludeTripId) " +
            "and t.plannedDepartureOrigin < :periodEnd and t.plannedArrivalOrigin > :periodStart")
    boolean existsDriverOverlap(@Param("driverId") Long driverId,
                                @Param("periodStart") LocalDateTime periodStart,
                                @Param("periodEnd") LocalDateTime periodEnd,
                                @Param("excludeTripId") Long excludeTripId);

    @Query("select t.id from Trip t where t.vehicle.id = :vehicleId " +
            "and (:excludeTripId is null or t.id != :excludeTripId) " +
            "and t.plannedDepartureOrigin < :periodEnd and t.plannedArrivalOrigin > :periodStart " +
            "order by t.plannedDepartureOrigin")
    List<Long> findVehicleOverlapIds(@Param("vehicleId") Long vehicleId,
                                     @Param("periodStart") LocalDateTime periodStart,
                                     @Param("periodEnd") LocalDateTime periodEnd,
                                     @Param("excludeTripId") Long excludeTripId);

    @Query("select t.id from Trip t join t.drivers d where d.id = :driverId " +
            "and (:excludeTripId is null or t.id != :excludeTripId) " +
            "and t.plannedDepartureOrigin < :periodEnd and t.plannedArrivalOrigin > :periodStart " +
            "order by t.plannedDepartureOrigin")
    List<Long> findDriverOverlapIds(@Param("driverId") Long driverId,
                                    @Param("periodStart") LocalDateTime periodStart,
                                    @Param("periodEnd") LocalDateTime periodEnd,
                                    @Param("excludeTripId") Long excludeTripId);
}
