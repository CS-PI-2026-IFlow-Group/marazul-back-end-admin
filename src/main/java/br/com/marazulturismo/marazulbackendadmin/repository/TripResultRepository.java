package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.dto.TripResultDTO;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripResultRepository extends Repository<Trip, Long> {

    String SELECT_RESULT = """
            select new br.com.marazulturismo.marazulbackendadmin.dto.TripResultDTO(
                t.id, t.plannedDepartureOrigin,
                origin.name, originState.acronym, destination.name, destinationState.acronym,
                client.name, t.status, t.estimatedTotalValue,
                (select sum(tp.amountPaid) from TripPassenger tp where tp.trip = t),
                (select sum(e.amount) from Expense e where e.trip = t))
            from Trip t
            join t.originCity origin
            join origin.state originState
            join t.destinationCity destination
            join destination.state destinationState
            left join t.client client
            """;

    @Query(SELECT_RESULT + """
            where t.plannedDepartureOrigin >= :periodStart
              and t.plannedDepartureOrigin < :periodEnd
              and (:clientId is null or client.id = :clientId)
            order by t.plannedDepartureOrigin asc, t.id asc
            """)
    List<TripResultDTO> findResultsBetween(@Param("periodStart") LocalDateTime periodStart,
                                           @Param("periodEnd") LocalDateTime periodEnd,
                                           @Param("clientId") Long clientId);

    @Query(SELECT_RESULT + "where t.id = :tripId")
    Optional<TripResultDTO> findResultByTripId(@Param("tripId") Long tripId);
}
