package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.DashboardMetricsResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PassengerRepository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CollaboratorRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void metrics_returnsCountsFromRepositories() {
        when(vehicleRepository.count()).thenReturn(12L);
        when(userRepository.countActiveEmployees()).thenReturn(7L);
        when(clientRepository.count()).thenReturn(4L);
        when(passengerRepository.count()).thenReturn(3L);

        DashboardMetricsResponseDTO metrics = dashboardService.metrics();

        assertThat(metrics.totalVehicles()).isEqualTo(12L);
        assertThat(metrics.activeEmployees()).isEqualTo(7L);
        assertThat(metrics.totalClients()).isEqualTo(4L);
        assertThat(metrics.totalPassengers()).isEqualTo(3L);
    }

    @Test
    void metrics_neverLoadsFullListsFromDatabase() {
        when(vehicleRepository.count()).thenReturn(12L);
        when(userRepository.countActiveEmployees()).thenReturn(7L);

        dashboardService.metrics();

        verify(vehicleRepository, never()).findAll();
        verify(vehicleRepository, never()).findByStatus(any());
        verify(userRepository, never()).findAll();
        verify(clientRepository, never()).findAll();
        verify(passengerRepository, never()).findAll();
    }

    @Test
    void metrics_returnsOtherCountsWhenOneFails() {
        when(vehicleRepository.count()).thenThrow(new DataAccessResourceFailureException("unavailable"));
        when(clientRepository.count()).thenReturn(4L);
        when(passengerRepository.count()).thenReturn(3L);

        DashboardMetricsResponseDTO metrics = dashboardService.metrics();

        assertThat(metrics.totalVehicles()).isNull();
        assertThat(metrics.totalClients()).isEqualTo(4L);
        assertThat(metrics.totalPassengers()).isEqualTo(3L);
    }

    @Test
    void metrics_returnsZerosWhenThereIsNoData() {
        when(vehicleRepository.count()).thenReturn(0L);
        when(userRepository.countActiveEmployees()).thenReturn(0L);

        DashboardMetricsResponseDTO metrics = dashboardService.metrics();

        assertThat(metrics.totalVehicles()).isZero();
        assertThat(metrics.activeEmployees()).isZero();
    }
}
