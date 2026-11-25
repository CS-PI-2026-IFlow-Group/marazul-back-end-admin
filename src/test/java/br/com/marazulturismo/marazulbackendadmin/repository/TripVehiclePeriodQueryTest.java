package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.dto.TripVehiclePeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TripVehiclePeriodQueryTest {

    private static final LocalDate PERIOD_START = LocalDate.of(2031, 3, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2031, 3, 31);

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private Vehicle vehicle;
    private City origin;
    private City destination;
    private Collaborator driver;

    @BeforeEach
    void setUp() {
        List<City> cities = cityRepository.findAll();
        origin = cities.get(0);
        destination = cities.get(1);

        vehicle = vehicleRepository.save(new Vehicle(
                "7201", "HUS7A01", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2022, 46, null, VehicleStatus.ACTIVE));

        Profile adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        driver = collaboratorRepository.save(new Collaborator(
                "Motorista Uso Frota", new Date(), Position.DRIVER,
                false, adminProfile, null, null, null, null));
    }

    @Test
    void findVehiclePeriodsOverlapping_withTripInsidePeriod_returnsVehicleAndPlannedDates() {
        LocalDateTime departure = LocalDateTime.of(2031, 3, 10, 8, 0);
        LocalDateTime arrival = LocalDateTime.of(2031, 3, 12, 18, 0);
        saveTrip(departure, arrival);

        assertThat(findForVehicle())
                .containsExactly(new TripVehiclePeriodDTO(vehicle.getId(), departure, arrival));
    }

    @Test
    void findVehiclePeriodsOverlapping_withTripCrossingPeriodBoundaries_returnsTrip() {
        saveTrip(LocalDateTime.of(2031, 2, 27, 8, 0), LocalDateTime.of(2031, 3, 2, 18, 0));
        saveTrip(LocalDateTime.of(2031, 3, 30, 8, 0), LocalDateTime.of(2031, 4, 2, 18, 0));

        assertThat(findForVehicle()).hasSize(2);
    }

    @Test
    void findVehiclePeriodsOverlapping_withReturnAtPeriodStart_returnsTrip() {
        saveTrip(LocalDateTime.of(2031, 2, 27, 8, 0), PERIOD_START.atStartOfDay());

        assertThat(findForVehicle()).hasSize(1);
    }

    @Test
    void findVehiclePeriodsOverlapping_withTripOutsidePeriod_ignoresTrip() {
        saveTrip(LocalDateTime.of(2031, 2, 20, 8, 0), LocalDateTime.of(2031, 2, 28, 23, 59));
        saveTrip(PERIOD_END.plusDays(1).atStartOfDay(), LocalDateTime.of(2031, 4, 3, 18, 0));

        assertThat(findForVehicle()).isEmpty();
    }

    private void saveTrip(LocalDateTime departure, LocalDateTime arrival) {
        LocalDateTime middle = departure.plusMinutes(Duration.between(departure, arrival).toMinutes() / 2);
        tripRepository.save(new Trip(origin, destination,
                departure, middle.minusMinutes(1),
                middle, arrival,
                vehicle, Set.of(driver)));
    }

    private List<TripVehiclePeriodDTO> findForVehicle() {
        return tripRepository.findVehiclePeriodsOverlapping(
                        PERIOD_START.atStartOfDay(), PERIOD_END.plusDays(1).atStartOfDay())
                .stream()
                .filter(it -> it.vehicleId().equals(vehicle.getId()))
                .toList();
    }
}
