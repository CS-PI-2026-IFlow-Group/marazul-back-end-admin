package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.DashboardMetricsResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PassengerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import java.util.function.LongSupplier;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private final VehicleRepository vehicleRepository;
    private final CollaboratorRepository userRepository;
    private final ClientRepository clientRepository;
    private final PassengerRepository passengerRepository;

    public DashboardService(VehicleRepository vehicleRepository, CollaboratorRepository userRepository,
                            ClientRepository clientRepository, PassengerRepository passengerRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.passengerRepository = passengerRepository;
    }

    public DashboardMetricsResponseDTO metrics() {
        return new DashboardMetricsResponseDTO(
                count("veículos", vehicleRepository::count),
                count("colaboradores ativos", userRepository::countActiveEmployees),
                count("clientes", clientRepository::count),
                count("passageiros", passengerRepository::count)
        );
    }

    private Long count(String metric, LongSupplier query) {
        try {
            return query.getAsLong();
        } catch (DataAccessException ex) {
            log.error("Falha ao consultar a contagem de {}", metric, ex);
            return null;
        }
    }
}
