package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.FleetEnumsResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.VehicleRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.VehicleResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.exception.LicensePlateAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.VehicleNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FleetService {

    private final VehicleRepository vehicleRepository;

    public FleetService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public List<VehicleResponseDTO> list(VehicleStatus status) {
        List<Vehicle> vehicles = status == null
                ? vehicleRepository.findAll()
                : vehicleRepository.findByStatus(status);

        return vehicles.stream()
                .map(VehicleResponseDTO::fromEntity)
                .toList();
    }

    public FleetEnumsResponseDTO listEnums() {
        return FleetEnumsResponseDTO.build();
    }

    @Transactional(readOnly = true)
    public VehicleResponseDTO findById(Long id) {
        return VehicleResponseDTO.fromEntity(findEntity(id));
    }

    @Transactional
    public VehicleResponseDTO create(VehicleRequestDTO dto) {
        String licensePlate = normalizeLicensePlate(dto.licensePlate());

        if (vehicleRepository.existsByLicensePlate(licensePlate)) {
            throw new LicensePlateAlreadyExistsException(licensePlate);
        }

        Vehicle vehicle = new Vehicle(
                dto.prefix(),
                licensePlate,
                dto.model(),
                dto.type(),
                dto.year(),
                dto.seats(),
                dto.inspectionDate(),
                dto.status()
        );

        return VehicleResponseDTO.fromEntity(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponseDTO update(Long id, VehicleRequestDTO dto) {
        Vehicle vehicle = findEntity(id);
        String licensePlate = normalizeLicensePlate(dto.licensePlate());

        if (vehicleRepository.existsByLicensePlateAndIdNot(licensePlate, id)) {
            throw new LicensePlateAlreadyExistsException(licensePlate);
        }

        vehicle.update(
                dto.prefix(),
                licensePlate,
                dto.model(),
                dto.type(),
                dto.year(),
                dto.seats(),
                dto.inspectionDate(),
                dto.status()
        );

        return VehicleResponseDTO.fromEntity(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = findEntity(id);
        vehicle.deactivate();
        vehicleRepository.save(vehicle);
    }

    private Vehicle findEntity(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    private static String normalizeLicensePlate(String licensePlate) {
        return VehicleRequestDTO.normalizeLicensePlate(licensePlate);
    }
}
