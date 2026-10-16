package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.VehicleRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.VehicleResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.exception.LicensePlateAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.VehicleNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FleetServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private FleetService fleetService;

    private VehicleRequestDTO requestWithoutStatus() {
        return new VehicleRequestDTO(
                "1001",
                "ABC1D23",
                BodyworkModel.MARCOPOLO,
                VehicleType.LD,
                2022,
                46,
                null,
                null
        );
    }

    @Test
    void create_assignsActiveStatusWhenNotProvided() {
        when(vehicleRepository.existsByLicensePlate("ABC1D23")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleResponseDTO response = fleetService.create(requestWithoutStatus());

        assertThat(response.status()).isEqualTo(VehicleStatus.ACTIVE);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(VehicleStatus.ACTIVE);
    }

    @Test
    void create_persistsNormalizedLicensePlate() {
        when(vehicleRepository.existsByLicensePlate("ABC1D23")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleRequestDTO dto = new VehicleRequestDTO(
                "1001", "  abc1d23 ",
                BodyworkModel.MARCOPOLO, VehicleType.LD,
                2022, 46, null, null);

        VehicleResponseDTO response = fleetService.create(dto);

        assertThat(response.licensePlate()).isEqualTo("ABC1D23");
    }

    @Test
    void create_persistsLicensePlateWithoutHyphen() {
        when(vehicleRepository.existsByLicensePlate("ABC1234")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleRequestDTO dto = new VehicleRequestDTO(
                "1001", "abc-1234",
                BodyworkModel.MARCOPOLO, VehicleType.LD,
                2022, 46, null, null);

        VehicleResponseDTO response = fleetService.create(dto);

        assertThat(response.licensePlate()).isEqualTo("ABC1234");
    }

    @Test
    void create_rejectsDuplicateLicensePlate() {
        when(vehicleRepository.existsByLicensePlate("ABC1D23")).thenReturn(true);

        assertThatThrownBy(() -> fleetService.create(requestWithoutStatus()))
                .isInstanceOf(LicensePlateAlreadyExistsException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void delete_performsLogicalDeactivation() {
        Vehicle vehicle = new Vehicle(
                "1001", "ABC1D23",
                BodyworkModel.BUSSCAR, VehicleType.DD,
                2021, 50, null, VehicleStatus.ACTIVE);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        fleetService.delete(1L);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.INACTIVE);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void findById_throwsNotFoundWhenMissing() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fleetService.findById(99L))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void update_rejectsLicensePlateOfAnotherVehicle() {
        Vehicle vehicle = new Vehicle(
                "1001", "ABC1D23",
                BodyworkModel.COMIL, VehicleType.CONVENTIONAL,
                2020, 44, null, VehicleStatus.ACTIVE);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByLicensePlateAndIdNot("XYZ9K88", 1L)).thenReturn(true);

        VehicleRequestDTO dto = new VehicleRequestDTO(
                "1001", "XYZ9K88",
                BodyworkModel.COMIL, VehicleType.CONVENTIONAL,
                2020, 44, null, VehicleStatus.ACTIVE);

        assertThatThrownBy(() -> fleetService.update(1L, dto))
                .isInstanceOf(LicensePlateAlreadyExistsException.class);

        verify(vehicleRepository, never()).save(any());
    }
}
