package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripPassengerRepository extends JpaRepository<TripPassenger, Long> {

    boolean existsByPassengerId(Long passengerId);

    List<TripPassenger> findByTripId(Long tripId);

    @Query("select tp from TripPassenger tp join fetch tp.passenger where tp.trip.id = :tripId order by tp.passenger.id")
    List<TripPassenger> findForReceiptsByTripId(@Param("tripId") Long tripId);

    @Query("select tp from TripPassenger tp join fetch tp.passenger "
            + "where tp.trip.id = :tripId and tp.passenger.id = :passengerId")
    Optional<TripPassenger> findForReceipt(@Param("tripId") Long tripId, @Param("passengerId") Long passengerId);

    boolean existsByTripIdAndPassengerId(Long tripId, Long passengerId);
}
