package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.DashboardMetricsResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final CollaboratorRepository userRepository;

    public DashboardService(VehicleRepository vehicleRepository, CollaboratorRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsResponseDTO metrics() {
        return new DashboardMetricsResponseDTO(
                vehicleRepository.count(),
                userRepository.countActiveEmployees()
        );
    }
}
