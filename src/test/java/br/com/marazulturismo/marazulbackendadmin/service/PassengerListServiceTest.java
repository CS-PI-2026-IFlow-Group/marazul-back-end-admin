package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerListItemDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.PassengerListResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.exception.TripNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.TripValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Transactional
class PassengerListServiceTest {

    private static final LocalDateTime DEPARTURE_ORIGIN = LocalDate.now().plusDays(30).atTime(7, 30);

    @Autowired
    private PassengerListService passengerListService;

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

    private City origin;
    private City destination;
    private Vehicle vehicle;
    private Collaborator driverOne;
    private Collaborator driverTwo;

    @BeforeEach
    void setUp() {
        List<City> cities = cityRepository.findAll();
        origin = cities.get(0);
        destination = cities.get(1);
        vehicle = vehicleRepository.save(new Vehicle(
                "LP01", "LPS0A01", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2023, 44, null, VehicleStatus.ACTIVE));
        driverOne = saveDriver("Zeca Motorista", "12345678900");
        driverTwo = saveDriver("Alberto Motorista", "98765432100");
    }

    @Test
    void generate_numbersPassengersAlphabeticallyAndHighlightsTheResponsible() {
        Passenger carla = savePassenger("Carla Souza", "900.000.000-01", "(51) 99988-7766");
        Passenger ana = savePassenger("Ana Lima", "900.000.000-02", null);
        Passenger bruno = savePassenger("Bruno Ávila", "900.000.000-03", "51 3333-4444");
        Trip trip = saveTrip(List.of(carla, ana, bruno), bruno);

        PassengerListResponseDTO list = passengerListService.generate(trip.getId());

        assertThat(list.passengers())
                .extracting(PassengerListItemDTO::number, PassengerListItemDTO::name)
                .containsExactly(
                        tuple(1, "Ana Lima"),
                        tuple(2, "Bruno Ávila"),
                        tuple(3, "Carla Souza"));
        assertThat(list.passengers()).filteredOn(PassengerListItemDTO::responsible)
                .extracting(PassengerListItemDTO::name)
                .containsExactly("Bruno Ávila");
        assertThat(list.responsible().name()).isEqualTo("Bruno Ávila");
        assertThat(list.responsible().phone()).isEqualTo("5133334444");
        assertThat(list.passengers().get(0).cpf()).isEqualTo("90000000002");
        assertThat(list.totalPassengers()).isEqualTo(3);
    }

    @Test
    void generate_bringsRouteDepartureVehicleDriversAndIssueDate() {
        Trip trip = saveTrip(List.of(savePassenger("Ana Lima", "900.000.000-04", null)), null);

        PassengerListResponseDTO list = passengerListService.generate(trip.getId());

        assertThat(list.tripId()).isEqualTo(trip.getId());
        assertThat(list.originCity()).isEqualTo(origin.getName());
        assertThat(list.originState()).isEqualTo(origin.getState().getAcronym());
        assertThat(list.destinationCity()).isEqualTo(destination.getName());
        assertThat(list.destinationState()).isEqualTo(destination.getState().getAcronym());
        assertThat(list.plannedDepartureOrigin()).isEqualTo(DEPARTURE_ORIGIN);
        assertThat(list.vehiclePrefix()).isEqualTo("LP01");
        assertThat(list.vehicleLicensePlate()).isEqualTo(vehicle.getLicensePlate());
        assertThat(list.drivers()).containsExactly("Alberto Motorista", "Zeca Motorista");
        assertThat(list.issuedAt()).isNotNull();
    }

    @Test
    void generate_withoutResponsible_leavesTheHighlightEmpty() {
        Trip trip = saveTrip(List.of(savePassenger("Ana Lima", "900.000.000-05", null)), null);

        PassengerListResponseDTO list = passengerListService.generate(trip.getId());

        assertThat(list.responsible()).isNull();
        assertThat(list.passengers()).noneMatch(PassengerListItemDTO::responsible);
    }

    @Test
    void generate_forTripWithoutPassengers_isRejectedWithTheReason() {
        Trip trip = saveTrip(List.of(), null);

        assertThatThrownBy(() -> passengerListService.generate(trip.getId()))
                .isInstanceOf(TripValidationException.class)
                .hasMessageContaining("não tem passageiros vinculados");
    }

    @Test
    void generate_forUnknownTrip_throwsNotFound() {
        assertThatThrownBy(() -> passengerListService.generate(999999L))
                .isInstanceOf(TripNotFoundException.class);
    }

    private Trip saveTrip(List<Passenger> passengers, Passenger responsible) {
        Trip trip = new Trip(origin, destination,
                DEPARTURE_ORIGIN, DEPARTURE_ORIGIN.plusHours(4),
                DEPARTURE_ORIGIN.plusDays(2), DEPARTURE_ORIGIN.plusDays(2).plusHours(4),
                vehicle, Set.of(driverOne, driverTwo));
        passengers.forEach(passenger -> trip.addPassenger(new TripPassenger(trip, passenger, new BigDecimal("100.00"))));
        trip.setResponsiblePassenger(responsible);
        return tripRepository.saveAndFlush(trip);
    }

    private Passenger savePassenger(String name, String cpf, String phone) {
        return passengerRepository.save(new Passenger(name, cpf, phone));
    }

    private Collaborator saveDriver(String name, String cnhNumber) {
        return collaboratorRepository.save(new Collaborator(
                name, new Date(), Position.DRIVER, false,
                profileRepository.findByName("Administrador").orElseThrow(),
                null, null, new CNH(cnhNumber, CNHType.D), null));
    }
}
