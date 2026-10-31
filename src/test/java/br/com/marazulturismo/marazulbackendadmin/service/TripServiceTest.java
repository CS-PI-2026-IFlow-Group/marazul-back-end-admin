package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.TripPassengerRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.exception.PassengerNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.PassengerValidationException;
import br.com.marazulturismo.marazulbackendadmin.exception.TripValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PassengerRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class TripServiceTest {

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

    private Vehicle vehicle;
    private Collaborator driver;
    private Passenger passengerOne;
    private Passenger passengerTwo;

    @BeforeEach
    void setUp() {
        var cities = cityRepository.findAll();
        vehicle = vehicleRepository.save(new Vehicle(
                "TEST", "TST0A01", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2023, 2, null, VehicleStatus.ACTIVE));

        Profile profile = profileRepository.findByName("Administrador").orElseThrow();
        driver = collaboratorRepository.save(new Collaborator(
                "Motorista Teste", new Date(), Position.DRIVER,
                false, profile, null, null, null, null));

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
                LocalDateTime.of(2026, 5, 1, 8, 0),
                LocalDateTime.of(2026, 5, 1, 12, 0),
                LocalDateTime.of(2026, 5, 3, 14, 0),
                LocalDateTime.of(2026, 5, 3, 18, 0),
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
                LocalDateTime.of(2026, 5, 1, 8, 0), LocalDateTime.of(2026, 5, 1, 12, 0),
                LocalDateTime.of(2026, 5, 3, 14, 0), LocalDateTime.of(2026, 5, 3, 18, 0),
                vehicle, Set.of(driver));
        trip.addPassenger(new TripPassenger(trip, passengerOne, new BigDecimal("100.00")));
        tripRepository.saveAndFlush(trip);

        assertThatThrownBy(() -> passengerService.delete(passengerOne.getId()))
                .isInstanceOf(PassengerValidationException.class)
                .hasMessageContaining("vinculado");
        assertThat(passengerRepository.findById(passengerOne.getId())).isPresent();
    }

    private TripRequestDTO request(BigDecimal estimatedAmount,
                                   List<TripPassengerRequestDTO> passengers,
                                   Long responsiblePassengerId) {
        var cities = cityRepository.findAll();
        return new TripRequestDTO(
                cities.get(0).getId(),
                cities.get(1).getId(),
                LocalDateTime.of(2026, 5, 1, 8, 0),
                LocalDateTime.of(2026, 5, 1, 12, 0),
                LocalDateTime.of(2026, 5, 3, 14, 0),
                LocalDateTime.of(2026, 5, 3, 18, 0),
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
