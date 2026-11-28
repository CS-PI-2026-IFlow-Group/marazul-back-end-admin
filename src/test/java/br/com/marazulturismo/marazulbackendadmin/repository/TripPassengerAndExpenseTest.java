package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TripPassengerAndExpenseTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripPassengerRepository tripPassengerRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private EntityManager entityManager;

    private City origin;
    private City destination;
    private Vehicle vehicle;
    private Collaborator driver;
    private Passenger passenger1;
    private Passenger passenger2;

    @BeforeEach
    void setUp() {
        var cities = cityRepository.findAll();
        origin = cities.get(0);
        destination = cities.get(1);

        vehicle = vehicleRepository.save(new Vehicle(
                "2001", "EXP0A01", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2023, 46, null, VehicleStatus.ACTIVE));

        Profile adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        driver = collaboratorRepository.save(new Collaborator(
                "Motorista Expedicao", new Date(), Position.DRIVER,
                false, adminProfile, null, null, null, null));

        passenger1 = passengerRepository.save(new Passenger("Passageiro Um", "111.222.333-44", "11988887777"));
        passenger2 = passengerRepository.save(new Passenger("Passageiro Dois", "222.333.444-55", "11977776666"));
    }

    private Trip createBaseTrip() {
        return new Trip(
                origin, destination,
                LocalDateTime.of(2026, 5, 1, 8, 0),
                LocalDateTime.of(2026, 5, 1, 12, 0),
                LocalDateTime.of(2026, 5, 3, 14, 0),
                LocalDateTime.of(2026, 5, 3, 18, 0),
                vehicle,
                Set.of(driver)
        );
    }

    @Test
    @DisplayName("Persiste passageiros e gastos em cascata a partir da viagem")
    void cascadePersist_savesPassengersAndExpensesWithTrip() {
        Trip trip = createBaseTrip();

        TripPassenger tp = new TripPassenger(trip, passenger1, new BigDecimal("250.00"), new BigDecimal("100.00"));
        Expense expense = new Expense(trip, "Pedágio ida e volta", new BigDecimal("78.50"));

        trip.addPassenger(tp);
        trip.addExpense(expense);

        Trip saved = tripRepository.save(trip);
        entityManager.flush();
        entityManager.clear();

        List<TripPassenger> savedPassengers = tripPassengerRepository.findByTripId(saved.getId());
        assertThat(savedPassengers).hasSize(1);
        assertThat(savedPassengers.get(0).getPassenger().getId()).isEqualTo(passenger1.getId());
        assertThat(savedPassengers.get(0).getAmountDue()).isEqualByComparingTo("250.00");
        assertThat(savedPassengers.get(0).getAmountPaid()).isEqualByComparingTo("100.00");

        List<Expense> savedExpenses = expenseRepository.findByTripId(saved.getId());
        assertThat(savedExpenses).hasSize(1);
        assertThat(savedExpenses.get(0).getDescription()).isEqualTo("Pedágio ida e volta");
        assertThat(savedExpenses.get(0).getAmount()).isEqualByComparingTo("78.50");
    }

    @Test
    @DisplayName("Substituição com orphanRemoval remove itens órfãos do banco")
    void orphanRemoval_removesDanglingPassengersAndExpenses() {
        Trip trip = createBaseTrip();
        trip.addPassenger(new TripPassenger(trip, passenger1, new BigDecimal("200.00")));
        trip.addExpense(new Expense(trip, "Gasto Inicial", new BigDecimal("50.00")));

        Trip saved = tripRepository.saveAndFlush(trip);
        Long tripId = saved.getId();
        entityManager.clear();

        Trip tripToUpdate = tripRepository.findById(tripId).orElseThrow();
        tripToUpdate.replacePassengers(List.of(new TripPassenger(null, passenger2, new BigDecimal("300.00"))));
        tripToUpdate.replaceExpenses(List.of(new Expense("Novo Gasto", new BigDecimal("120.00"))));

        tripRepository.saveAndFlush(tripToUpdate);
        entityManager.clear();

        List<TripPassenger> passengersAfter = tripPassengerRepository.findByTripId(tripId);
        assertThat(passengersAfter).hasSize(1);
        assertThat(passengersAfter.get(0).getPassenger().getId()).isEqualTo(passenger2.getId());

        List<Expense> expensesAfter = expenseRepository.findByTripId(tripId);
        assertThat(expensesAfter).hasSize(1);
        assertThat(expensesAfter.get(0).getDescription()).isEqualTo("Novo Gasto");
    }

    @Test
    @DisplayName("Exclusão da viagem remove trip_passengers e expenses sem excluir o cadastro do passageiro")
    void tripDeletion_cascadesToPassengersAndExpenses_withoutDeletingPassenger() {
        Trip trip = createBaseTrip();
        trip.addPassenger(new TripPassenger(trip, passenger1, new BigDecimal("200.00")));
        trip.addExpense(new Expense(trip, "Combustível", new BigDecimal("350.00")));

        Trip saved = tripRepository.saveAndFlush(trip);
        Long tripId = saved.getId();
        Long passengerId = passenger1.getId();

        tripRepository.delete(saved);
        entityManager.flush();
        entityManager.clear();

        assertThat(tripRepository.findById(tripId)).isEmpty();
        assertThat(tripPassengerRepository.findByTripId(tripId)).isEmpty();
        assertThat(expenseRepository.findByTripId(tripId)).isEmpty();

        // O cadastro do passageiro deve continuar intacto
        assertThat(passengerRepository.findById(passengerId)).isPresent();
    }

    @Test
    @DisplayName("Restrição de unicidade impede passageiro duplicado na mesma viagem")
    void uniqueConstraint_preventsDuplicatePassengerInSameTrip() {
        Trip trip = createBaseTrip();
        trip.addPassenger(new TripPassenger(trip, passenger1, new BigDecimal("100.00")));
        trip.addPassenger(new TripPassenger(trip, passenger1, new BigDecimal("150.00")));

        assertThatThrownBy(() -> tripRepository.saveAndFlush(trip))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("existsByPassengerId retorna true apenas se o passageiro estiver em alguma viagem")
    void existsByPassengerId_detectsLinkedPassenger() {
        Trip trip = createBaseTrip();
        trip.addPassenger(new TripPassenger(trip, passenger1, new BigDecimal("200.00")));

        tripRepository.saveAndFlush(trip);
        entityManager.clear();

        assertThat(tripPassengerRepository.existsByPassengerId(passenger1.getId())).isTrue();
        assertThat(tripPassengerRepository.existsByPassengerId(passenger2.getId())).isFalse();
    }

    @Test
    @DisplayName("amountPaid inicia em zero por padrão e amountDue aceita nulo")
    void amountDefaults_areRespected() {
        Trip trip = createBaseTrip();
        trip.addPassenger(new TripPassenger(trip, passenger1, null));

        Trip saved = tripRepository.saveAndFlush(trip);
        entityManager.clear();

        TripPassenger loaded = tripPassengerRepository.findByTripId(saved.getId()).get(0);
        assertThat(loaded.getAmountDue()).isNull();
        assertThat(loaded.getAmountPaid()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
