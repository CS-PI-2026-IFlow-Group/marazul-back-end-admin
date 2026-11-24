package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface MonthlySummaryRepository extends JpaRepository<Trip, Long> {

    @Query(value = """
            SELECT YEAR(t.planned_departure_origin) AS ano,
                   MONTH(t.planned_departure_origin) AS mes,
                   COUNT(DISTINCT t.id) AS viagens,
                   COALESCE(SUM(passenger_counts.quantidade), 0) AS passageiros,
                   COALESCE(MAX(recebido.total), 0) AS recebido,
                   COALESCE(MAX(gastos.total), 0) AS gastos
            FROM trips t
            LEFT JOIN (
                SELECT trip_id, COUNT(*) AS quantidade
                FROM trip_passengers
                GROUP BY trip_id
            ) passenger_counts ON passenger_counts.trip_id = t.id
            LEFT JOIN (
                SELECT YEAR(received_trip.planned_departure_origin) AS ano,
                       MONTH(received_trip.planned_departure_origin) AS mes,
                       SUM(trip_passenger.amount_paid) AS total
                FROM trips received_trip
                JOIN trip_passengers trip_passenger ON trip_passenger.trip_id = received_trip.id
                WHERE received_trip.status = 'COMPLETED'
                  AND received_trip.planned_departure_origin >= :periodStart
                  AND received_trip.planned_departure_origin < :periodEnd
                GROUP BY YEAR(received_trip.planned_departure_origin),
                         MONTH(received_trip.planned_departure_origin)
            ) recebido ON recebido.ano = YEAR(t.planned_departure_origin)
                      AND recebido.mes = MONTH(t.planned_departure_origin)
            LEFT JOIN (
                SELECT YEAR(expense_trip.planned_departure_origin) AS ano,
                       MONTH(expense_trip.planned_departure_origin) AS mes,
                       SUM(expense.amount) AS total
                FROM trips expense_trip
                JOIN expenses expense ON expense.trip_id = expense_trip.id
                WHERE expense_trip.status = 'COMPLETED'
                  AND expense_trip.planned_departure_origin >= :periodStart
                  AND expense_trip.planned_departure_origin < :periodEnd
                GROUP BY YEAR(expense_trip.planned_departure_origin),
                         MONTH(expense_trip.planned_departure_origin)
            ) gastos ON gastos.ano = YEAR(t.planned_departure_origin)
                    AND gastos.mes = MONTH(t.planned_departure_origin)
            WHERE t.status = 'COMPLETED'
              AND t.planned_departure_origin >= :periodStart
              AND t.planned_departure_origin < :periodEnd
            GROUP BY YEAR(t.planned_departure_origin), MONTH(t.planned_departure_origin)
            ORDER BY ano, mes
            """, nativeQuery = true)
    List<MonthlyAggregate> summarizeMonths(@Param("periodStart") LocalDateTime periodStart,
                                          @Param("periodEnd") LocalDateTime periodEnd);

    @Query(value = """
            SELECT client.id AS clienteId,
                   client.name AS cliente,
                   COUNT(trip.id) AS viagens
            FROM trips trip
            JOIN clients client ON client.id = trip.client_id
            WHERE trip.status = 'COMPLETED'
              AND trip.planned_departure_origin >= :periodStart
              AND trip.planned_departure_origin < :periodEnd
            GROUP BY client.id, client.name
            ORDER BY COUNT(trip.id) DESC, client.name ASC
            LIMIT 5
            """, nativeQuery = true)
    List<TopClientAggregate> findTopClients(@Param("periodStart") LocalDateTime periodStart,
                                            @Param("periodEnd") LocalDateTime periodEnd);

    interface MonthlyAggregate {
        Integer getAno();

        Integer getMes();

        Long getViagens();

        Long getPassageiros();

        BigDecimal getRecebido();

        BigDecimal getGastos();
    }

    interface TopClientAggregate {
        Long getClienteId();

        String getCliente();

        Long getViagens();
    }
}
