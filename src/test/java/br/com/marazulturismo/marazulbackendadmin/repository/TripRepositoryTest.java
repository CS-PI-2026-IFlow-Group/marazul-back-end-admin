package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TripRepositoryTest {

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

    // Viagem base: 01/jan 08:00 → 01/jan 14:00 (ida) / 02/jan 08:00 → 02/jan 14:00 (volta)
    private static final LocalDateTime BASE_DEPARTURE_ORIGIN = LocalDateTime.of(2026, 1, 1, 8, 0);
    private static final LocalDateTime BASE_ARRIVAL_DESTINATION = LocalDateTime.of(2026, 1, 1, 14, 0);
    private static final LocalDateTime BASE_DEPARTURE_DESTINATION = LocalDateTime.of(2026, 1, 2, 8, 0);
    private static final LocalDateTime BASE_ARRIVAL_ORIGIN = LocalDateTime.of(2026, 1, 2, 14, 0);

    @BeforeEach
    void setUp() {
        var cities = cityRepository.findAll();
        origin = cities.get(0);
        destination = cities.get(1);

        vehicle = vehicleRepository.save(new Vehicle(
                "1001", "TST0A01", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2022, 46, null, VehicleStatus.ACTIVE));

        Profile adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        driver = collaboratorRepository.save(new Collaborator(
                "Motorista Teste", new Date(), Position.DRIVER,
                false, adminProfile, null, null, null, null));
    }

    @Test
    void vehicleOverlap_withOverlappingPeriod_returnsTrue() {
        tripRepository.save(new Trip(origin, destination,
                BASE_DEPARTURE_ORIGIN, BASE_ARRIVAL_DESTINATION,
                BASE_DEPARTURE_DESTINATION, BASE_ARRIVAL_ORIGIN,
                vehicle, Set.of(driver)));

        // Período que sobrepõe: começa antes do fim da viagem base
        boolean overlap = tripRepository.existsVehicleOverlap(
                vehicle.getId(),
                BASE_DEPARTURE_ORIGIN.plusHours(1),
                BASE_ARRIVAL_ORIGIN.minusHours(1),
                null);

        assertThat(overlap).isTrue();
    }

    @Test
    void vehicleOverlap_withNonOverlappingPeriod_returnsFalse() {
        tripRepository.save(new Trip(origin, destination,
                BASE_DEPARTURE_ORIGIN, BASE_ARRIVAL_DESTINATION,
                BASE_DEPARTURE_DESTINATION, BASE_ARRIVAL_ORIGIN,
                vehicle, Set.of(driver)));

        // Período totalmente depois: começa quando a viagem base já terminou
        boolean overlap = tripRepository.existsVehicleOverlap(
                vehicle.getId(),
                BASE_ARRIVAL_ORIGIN.plusHours(1),
                BASE_ARRIVAL_ORIGIN.plusHours(10),
                null);

        assertThat(overlap).isFalse();
    }

    @Test
    void vehicleOverlap_excludingOwnTrip_returnsFalse() {
        Trip trip = tripRepository.save(new Trip(origin, destination,
                BASE_DEPARTURE_ORIGIN, BASE_ARRIVAL_DESTINATION,
                BASE_DEPARTURE_DESTINATION, BASE_ARRIVAL_ORIGIN,
                vehicle, Set.of(driver)));

        // O mesmo período sobrepõe, mas excluímos a própria viagem
        boolean overlap = tripRepository.existsVehicleOverlap(
                vehicle.getId(),
                BASE_DEPARTURE_ORIGIN,
                BASE_ARRIVAL_ORIGIN,
                trip.getId());

        assertThat(overlap).isFalse();
    }

    @Test
    void driverOverlap_withOverlappingPeriod_returnsTrue() {
        tripRepository.save(new Trip(origin, destination,
                BASE_DEPARTURE_ORIGIN, BASE_ARRIVAL_DESTINATION,
                BASE_DEPARTURE_DESTINATION, BASE_ARRIVAL_ORIGIN,
                vehicle, Set.of(driver)));

        boolean overlap = tripRepository.existsDriverOverlap(
                driver.getId(),
                BASE_DEPARTURE_ORIGIN.plusHours(1),
                BASE_ARRIVAL_ORIGIN.minusHours(1),
                null);

        assertThat(overlap).isTrue();
    }

    @Test
    void driverOverlap_withNonOverlappingPeriod_returnsFalse() {
        tripRepository.save(new Trip(origin, destination,
                BASE_DEPARTURE_ORIGIN, BASE_ARRIVAL_DESTINATION,
                BASE_DEPARTURE_DESTINATION, BASE_ARRIVAL_ORIGIN,
                vehicle, Set.of(driver)));

        boolean overlap = tripRepository.existsDriverOverlap(
                driver.getId(),
                BASE_ARRIVAL_ORIGIN.plusHours(1),
                BASE_ARRIVAL_ORIGIN.plusHours(10),
                null);

        assertThat(overlap).isFalse();
    }

    @Test
    void driverOverlap_excludingOwnTrip_returnsFalse() {
        Trip trip = tripRepository.save(new Trip(origin, destination,
                BASE_DEPARTURE_ORIGIN, BASE_ARRIVAL_DESTINATION,
                BASE_DEPARTURE_DESTINATION, BASE_ARRIVAL_ORIGIN,
                vehicle, Set.of(driver)));

        boolean overlap = tripRepository.existsDriverOverlap(
                driver.getId(),
                BASE_DEPARTURE_ORIGIN,
                BASE_ARRIVAL_ORIGIN,
                trip.getId());

        assertThat(overlap).isFalse();
    }

    @Test
    void vehicleOverlap_withDifferentVehicle_returnsFalse() {
        tripRepository.save(new Trip(origin, destination,
                BASE_DEPARTURE_ORIGIN, BASE_ARRIVAL_DESTINATION,
                BASE_DEPARTURE_DESTINATION, BASE_ARRIVAL_ORIGIN,
                vehicle, Set.of(driver)));

        Vehicle otherVehicle = vehicleRepository.save(new Vehicle(
                "1002", "TST0B02", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2023, 46, null, VehicleStatus.ACTIVE));

        // Mesmo período, mas veículo diferente
        boolean overlap = tripRepository.existsVehicleOverlap(
                otherVehicle.getId(),
                BASE_DEPARTURE_ORIGIN,
                BASE_ARRIVAL_ORIGIN,
                null);

        assertThat(overlap).isFalse();
    }

    @Test
    void driverOverlap_withDifferentDriver_returnsFalse() {
        tripRepository.save(new Trip(origin, destination,
                BASE_DEPARTURE_ORIGIN, BASE_ARRIVAL_DESTINATION,
                BASE_DEPARTURE_DESTINATION, BASE_ARRIVAL_ORIGIN,
                vehicle, Set.of(driver)));

        Profile adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        Collaborator otherDriver = collaboratorRepository.save(new Collaborator(
                "Outro Motorista", new Date(), Position.DRIVER,
                false, adminProfile, null, null, null, null));

        // Mesmo período, mas motorista diferente
        boolean overlap = tripRepository.existsDriverOverlap(
                otherDriver.getId(),
                BASE_DEPARTURE_ORIGIN,
                BASE_ARRIVAL_ORIGIN,
                null);

        assertThat(overlap).isFalse();
    }
}
