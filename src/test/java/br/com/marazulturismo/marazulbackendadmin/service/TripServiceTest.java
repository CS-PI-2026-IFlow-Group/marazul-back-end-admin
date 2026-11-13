package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.TripPassengerRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.exception.PassengerNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.PassengerValidationException;
import br.com.marazulturismo.marazulbackendadmin.exception.TripValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.Client;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Expense;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.AddressRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ExpenseRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PassengerRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripPassengerRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TripServiceTest {

    private static final LocalDateTime DEPARTURE_ORIGIN = LocalDate.now().plusDays(30).atTime(8, 0);
    private static final LocalDateTime ARRIVAL_DESTINATION = DEPARTURE_ORIGIN.plusHours(4);
    private static final LocalDateTime DEPARTURE_DESTINATION = DEPARTURE_ORIGIN.plusDays(2).plusHours(6);
    private static final LocalDateTime ARRIVAL_ORIGIN = DEPARTURE_ORIGIN.plusDays(2).plusHours(10);

    @Autowired
    private TripService tripService;

    @Autowired
    private PassengerService passengerService;

    @Autowired
    private TripRepository tripRepository;

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
    private TripPassengerRepository tripPassengerRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AddressRepository addressRepository;

    private Vehicle vehicle;
    private Collaborator driver;
    private Passenger passengerOne;
    private Passenger passengerTwo;

    @BeforeEach
    void setUp() {
        vehicle = vehicleRepository.save(new Vehicle(
                "TEST", "TST0A01", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2023, 2, null, VehicleStatus.ACTIVE));

        Profile profile = profileRepository.findByName("Administrador").orElseThrow();
        driver = collaboratorRepository.save(new Collaborator(
                "Motorista Teste", new Date(), Position.DRIVER,
                false, profile, null, null, new CNH("12345678900", CNHType.D), null));

        passengerOne = passengerRepository.save(new Passenger("Passageiro Um", "111.222.333-44", null));
        passengerTwo = passengerRepository.save(new Passenger("Passageiro Dois", "222.333.444-55", null));
    }

    @Test
    void create_defaultsDueAmountAndCalculatesTotals() {
        TripResponseDTO response = tripService.create(request(
                new BigDecimal("150.00"),
                List.of(new TripPassengerRequestDTO(passengerOne.getId(), null, new BigDecimal("100.00"))),
                passengerOne.getId()));

        assertThat(response.passengers()).hasSize(1);
        assertThat(response.passengers().get(0).amountDue()).isEqualByComparingTo("150.00");
        assertThat(response.passengers().get(0).amountPaid()).isEqualByComparingTo("100.00");
        assertThat(response.totalReceived()).isEqualByComparingTo("100.00");
        assertThat(response.totalOpen()).isEqualByComparingTo("50.00");
        assertThat(response.responsiblePassengerId()).isEqualTo(passengerOne.getId());
    }

    @Test
    void create_rejectsDuplicatePassenger() {
        List<TripPassengerRequestDTO> passengers = List.of(
                new TripPassengerRequestDTO(passengerOne.getId(), new BigDecimal("100.00"), null),
                new TripPassengerRequestDTO(passengerOne.getId(), new BigDecimal("100.00"), null));

        assertThatThrownBy(() -> tripService.create(request(null, passengers, null)))
                .isInstanceOf(TripValidationException.class)
                .hasMessageContaining("mesmo passageiro");
    }

    @Test
    void create_rejectsUnknownPassenger() {
        List<TripPassengerRequestDTO> passengers = List.of(
                new TripPassengerRequestDTO(999999L, new BigDecimal("100.00"), null));

        assertThatThrownBy(() -> tripService.create(request(null, passengers, null)))
                .isInstanceOf(PassengerNotFoundException.class);
    }

    @Test
    void create_rejectsCapacityOverflowAndInvalidPayment() {
        List<TripPassengerRequestDTO> passengers = List.of(
                new TripPassengerRequestDTO(passengerOne.getId(), new BigDecimal("100.00"), null),
                new TripPassengerRequestDTO(passengerTwo.getId(), new BigDecimal("100.00"), null),
                new TripPassengerRequestDTO(999998L, new BigDecimal("100.00"), null));

        assertThatThrownBy(() -> tripService.create(request(null, passengers, null)))
                .isInstanceOf(TripValidationException.class)
                .hasMessageContaining("no máximo 2");

        assertThatThrownBy(() -> tripService.create(request(null, List.of(
                new TripPassengerRequestDTO(passengerOne.getId(), new BigDecimal("100.00"), new BigDecimal("101.00"))), null)))
                .isInstanceOf(TripValidationException.class)
                .hasMessageContaining("ultrapassar");

        assertThatThrownBy(() -> tripService.create(request(null, List.of(
                new TripPassengerRequestDTO(passengerOne.getId(), new BigDecimal("100.00"), new BigDecimal("-1.00"))), null)))
                .isInstanceOf(TripValidationException.class)
                .hasMessageContaining("não pode ser negativo");
    }

    @Test
    void update_withoutPassengersPreservesExistingList() {
        TripResponseDTO created = tripService.create(request(
                new BigDecimal("120.00"),
                List.of(new TripPassengerRequestDTO(passengerOne.getId(), null, null)),
                passengerOne.getId()));

        TripRequestDTO update = request(null, null, passengerOne.getId());
        TripResponseDTO updated = tripService.update(created.id(), update);

        assertThat(updated.passengers()).hasSize(1);
        assertThat(updated.passengers().get(0).passengerId()).isEqualTo(passengerOne.getId());
        assertThat(updated.responsiblePassengerId()).isEqualTo(passengerOne.getId());
    }

    @Test
    void create_rejectsResponsibleOutsidePassengerList() {
        assertThatThrownBy(() -> tripService.create(request(
                null,
                List.of(new TripPassengerRequestDTO(passengerOne.getId(), null, null)),
                passengerTwo.getId())))
                .isInstanceOf(TripValidationException.class)
                .hasMessageContaining("responsável");
    }

    @Test
    void confirm_rejectsBelowMinimumButSchedulingIsAllowed() {
        TripRequestDTO request = new TripRequestDTO(
                cityRepository.findAll().get(0).getId(),
                cityRepository.findAll().get(1).getId(),
                DEPARTURE_ORIGIN,
                ARRIVAL_DESTINATION,
                DEPARTURE_DESTINATION,
                ARRIVAL_ORIGIN,
                vehicle.getId(),
                Set.of(driver.getId()),
                null,
                2,
                null,
                null,
                List.of(new TripPassengerRequestDTO(passengerOne.getId(), null, null)),
                null);

        TripResponseDTO scheduled = tripService.create(request);
        assertThat(scheduled.status().name()).isEqualTo("SCHEDULED");
        assertThatThrownBy(() -> tripService.confirm(scheduled.id()))
                .isInstanceOf(TripValidationException.class)
                .hasMessageContaining("pelo menos 2");
    }

    @Test
    void deletePassenger_rejectsPassengerLinkedToTrip() {
        Trip trip = new Trip(
                cityRepository.findAll().get(0), cityRepository.findAll().get(1),
                DEPARTURE_ORIGIN, ARRIVAL_DESTINATION,
                DEPARTURE_DESTINATION, ARRIVAL_ORIGIN,
                vehicle, Set.of(driver));
        trip.addPassenger(new TripPassenger(trip, passengerOne, new BigDecimal("100.00")));
        tripRepository.saveAndFlush(trip);

        assertThatThrownBy(() -> passengerService.delete(passengerOne.getId()))
                .isInstanceOf(PassengerValidationException.class)
                .hasMessageContaining("vinculado");
        assertThat(passengerRepository.findById(passengerOne.getId())).isPresent();
    }

    @Test
    void update_changesTeamAndClientWithoutRemovingPassengersOrExpenses() {
        TripResponseDTO created = tripService.create(request(
                null, List.of(new TripPassengerRequestDTO(passengerOne.getId(), null, null)), null));
        Trip trip = tripRepository.findById(created.id()).orElseThrow();
        trip.addExpense(new Expense("Pedágio", new BigDecimal("80.00")));
        tripRepository.saveAndFlush(trip);

        Collaborator otherDriver = collaboratorRepository.save(new Collaborator(
                "Motorista Dois", new Date(), Position.DRIVER, false,
                profileRepository.findByName("Administrador").orElseThrow(),
                null, null, new CNH("98765432100", CNHType.D), null));
        Client client = saveClient();
        var cities = cityRepository.findAll();
        tripService.update(created.id(), new TripRequestDTO(
                cities.get(0).getId(), cities.get(1).getId(),
                DEPARTURE_ORIGIN, ARRIVAL_DESTINATION, DEPARTURE_DESTINATION, ARRIVAL_ORIGIN,
                vehicle.getId(), Set.of(otherDriver.getId()), client.getId(),
                1, null, "Troca de motorista", null, null));
        tripRepository.flush();

        Trip updated = tripRepository.findById(created.id()).orElseThrow();
        assertThat(updated.getDrivers()).extracting(Collaborator::getId).containsExactly(otherDriver.getId());
        assertThat(updated.getClient().getId()).isEqualTo(client.getId());
        assertThat(tripPassengerRepository.findByTripId(created.id())).hasSize(1);
        assertThat(expenseRepository.findByTripId(created.id())).hasSize(1);
    }

    @Test
    void update_toSmallerVehicleIsAllowedWhenTheNewPassengerListFits() {
        TripResponseDTO created = tripService.create(request(null, List.of(
                new TripPassengerRequestDTO(passengerOne.getId(), null, null),
                new TripPassengerRequestDTO(passengerTwo.getId(), null, null)), null));
        Vehicle smallVehicle = vehicleRepository.save(new Vehicle(
                "TST2", "TST0A02", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2023, 1, null, VehicleStatus.ACTIVE));

        Passenger passengerThree = passengerRepository.save(new Passenger("Passageiro Três", "333.444.555-66", null));

        TripRequestDTO base = request(null, List.of(
                new TripPassengerRequestDTO(passengerThree.getId(), null, null)), null);
        TripResponseDTO updated = tripService.update(created.id(), new TripRequestDTO(
                base.originCityId(), base.destinationCityId(),
                base.plannedDepartureOrigin(), base.plannedArrivalDestination(),
                base.plannedDepartureDestination(), base.plannedArrivalOrigin(),
                smallVehicle.getId(), base.driverIds(), null, null, null, null, base.passengers(), null));

        assertThat(updated.vehicleId()).isEqualTo(smallVehicle.getId());
        assertThat(updated.passengers()).hasSize(1);
    }

    @Test
    void update_resendingAlreadyLinkedPassengerKeepsTheLinkAndUpdatesAmounts() {
        TripResponseDTO created = tripService.create(request(new BigDecimal("100.00"), List.of(
                new TripPassengerRequestDTO(passengerOne.getId(), null, null)), null));
        Long linkId = tripPassengerRepository.findByTripId(created.id()).get(0).getId();

        TripResponseDTO updated = tripService.update(created.id(), request(new BigDecimal("100.00"), List.of(
                new TripPassengerRequestDTO(passengerOne.getId(), null, new BigDecimal("50.00")),
                new TripPassengerRequestDTO(passengerTwo.getId(), null, null)), null));
        tripRepository.flush();

        assertThat(updated.passengers()).hasSize(2);
        assertThat(updated.totalReceived()).isEqualByComparingTo("50.00");
        assertThat(tripPassengerRepository.findByTripId(created.id()))
                .extracting(TripPassenger::getId)
                .contains(linkId);
    }

    @Test
    void delete_removesPassengerLinksAndExpensesButKeepsPassengers() {
        TripResponseDTO created = tripService.create(request(null, List.of(
                new TripPassengerRequestDTO(passengerOne.getId(), null, null),
                new TripPassengerRequestDTO(passengerTwo.getId(), null, null)), null));
        Trip trip = tripRepository.findById(created.id()).orElseThrow();
        trip.addExpense(new Expense("Combustível", new BigDecimal("500.00")));
        tripRepository.saveAndFlush(trip);

        tripService.delete(created.id());
        tripRepository.flush();

        assertThat(tripRepository.findById(created.id())).isEmpty();
        assertThat(tripPassengerRepository.findByTripId(created.id())).isEmpty();
        assertThat(expenseRepository.findByTripId(created.id())).isEmpty();
        assertThat(passengerRepository.findById(passengerOne.getId())).isPresent();
        assertThat(passengerRepository.findById(passengerTwo.getId())).isPresent();
    }

    private Client saveClient() {
        Address address = addressRepository.save(
                new Address("Rua da Viagem", "56", null, cityRepository.findAll().get(0)));
        return clientRepository.save(new Client("Cliente da Viagem", "111.444.777-35", null, address));
    }

    private TripRequestDTO request(BigDecimal estimatedAmount,
                                   List<TripPassengerRequestDTO> passengers,
                                   Long responsiblePassengerId) {
        var cities = cityRepository.findAll();
        return new TripRequestDTO(
                cities.get(0).getId(),
                cities.get(1).getId(),
                DEPARTURE_ORIGIN,
                ARRIVAL_DESTINATION,
                DEPARTURE_DESTINATION,
                ARRIVAL_ORIGIN,
                vehicle.getId(),
                Set.of(driver.getId()),
                null,
                null,
                estimatedAmount,
                null,
                passengers,
                responsiblePassengerId);
    }
}
