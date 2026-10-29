package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripPassengerRepository extends JpaRepository<TripPassenger, Long> {

    boolean existsByPassengerId(Long passengerId);

    List<TripPassenger> findByTripId(Long tripId);

    boolean existsByTripIdAndPassengerId(Long tripId, Long passengerId);
}
