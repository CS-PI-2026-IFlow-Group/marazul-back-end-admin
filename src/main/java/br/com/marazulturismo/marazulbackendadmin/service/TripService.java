package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.TripPassengerRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.exception.CityNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.ClientNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.EmployeeNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.PassengerNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.TripNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.TripValidationException;
import br.com.marazulturismo.marazulbackendadmin.exception.VehicleNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PassengerRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TripService {

    private static final Set<TripStatus> EDITABLE_STATUSES = EnumSet.of(TripStatus.SCHEDULED, TripStatus.CONFIRMED);

    private final TripRepository tripRepository;
    private final CityRepository cityRepository;
    private final VehicleRepository vehicleRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final ClientRepository clientRepository;
    private final PassengerRepository passengerRepository;

    public TripService(TripRepository tripRepository, CityRepository cityRepository,
                       VehicleRepository vehicleRepository, CollaboratorRepository collaboratorRepository,
                       ClientRepository clientRepository, PassengerRepository passengerRepository) {
        this.tripRepository = tripRepository;
        this.cityRepository = cityRepository;
        this.vehicleRepository = vehicleRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.clientRepository = clientRepository;
        this.passengerRepository = passengerRepository;
    }

    @Transactional(readOnly = true)
    public List<TripResponseDTO> list() {
        return tripRepository.findAll().stream().map(TripResponseDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public TripResponseDTO findById(Long id) {
        return TripResponseDTO.fromEntity(findEntity(id));
    }

    @Transactional
    public TripResponseDTO create(TripRequestDTO dto) {
        validateRouteAndSchedule(dto);
        Vehicle vehicle = findVehicle(dto.vehicleId());
        validateVehicle(vehicle);
        Set<Collaborator> drivers = findDrivers(dto.driverIds());
        validateDrivers(drivers);
        validateScheduleConflicts(dto, vehicle, drivers, null);
        Trip trip = new Trip(
                findCity(dto.originCityId()),
                findCity(dto.destinationCityId()),
                dto.plannedDepartureOrigin(),
                dto.plannedArrivalDestination(),
                dto.plannedDepartureDestination(),
                dto.plannedArrivalOrigin(),
                vehicle,
                drivers,
                findClient(dto.clientId()),
                dto.minimumPassengers(),
                dto.notes());
        trip.update(trip.getOriginCity(), trip.getDestinationCity(), trip.getPlannedDepartureOrigin(),
                trip.getPlannedArrivalDestination(), trip.getPlannedDepartureDestination(),
                trip.getPlannedArrivalOrigin(), vehicle, trip.getDrivers(), trip.getClient(),
                dto.minimumPassengers(), dto.estimatedPassengerAmount(), dto.notes(), null, trip.getStatus());
        replacePassengersIfProvided(trip, dto.passengers());
        trip.setResponsiblePassenger(resolveResponsible(dto.responsiblePassengerId(), trip.getPassengers()));
        return TripResponseDTO.fromEntity(tripRepository.save(trip));
    }

    @Transactional
    public TripResponseDTO update(Long id, TripRequestDTO dto) {
        Trip trip = findEntity(id);
        validateNotStarted(trip);
        validateRouteAndSchedule(dto);
        Vehicle vehicle = findVehicle(dto.vehicleId());
        validateVehicle(vehicle);
        Set<Collaborator> drivers = findDrivers(dto.driverIds());
        validateDrivers(drivers);
        validateScheduleConflicts(dto, vehicle, drivers, trip.getId());
        trip.update(
                findCity(dto.originCityId()),
                findCity(dto.destinationCityId()),
                dto.plannedDepartureOrigin(),
                dto.plannedArrivalDestination(),
                dto.plannedDepartureDestination(),
                dto.plannedArrivalOrigin(),
                vehicle,
                drivers,
                findClient(dto.clientId()),
                dto.minimumPassengers(),
                dto.estimatedPassengerAmount(),
                dto.notes(),
                trip.getResponsiblePassenger(),
                trip.getStatus());
        replacePassengersIfProvided(trip, dto.passengers());
        trip.setResponsiblePassenger(resolveResponsible(dto.responsiblePassengerId(), trip.getPassengers()));
        return TripResponseDTO.fromEntity(tripRepository.save(trip));
    }

    @Transactional
    public void delete(Long id) {
        tripRepository.delete(findEntity(id));
    }

    @Transactional
    public TripResponseDTO confirm(Long id) {
        Trip trip = findEntity(id);
        if (trip.getMinimumPassengers() != null && trip.getPassengers().size() < trip.getMinimumPassengers()) {
            throw new TripValidationException("A viagem precisa de pelo menos "
                    + trip.getMinimumPassengers() + " passageiros para ser confirmada.");
        }
        trip.confirm();
        return TripResponseDTO.fromEntity(tripRepository.save(trip));
    }

    private void validateNotStarted(Trip trip) {
        if (!EDITABLE_STATUSES.contains(trip.getStatus())) {
            throw new TripValidationException("A viagem " + trip.getId() + " já saiu da origem e está em andamento ("
                    + trip.getStatus().getLabel().toLowerCase() + "), por isso não pode ser alterada.");
        }
    }

    private void validateRouteAndSchedule(TripRequestDTO dto) {
        if (dto.originCityId().equals(dto.destinationCityId())) {
            throw new TripValidationException("A cidade de destino deve ser diferente da cidade de origem.");
        }
        if (dto.plannedDepartureOrigin().isBefore(LocalDateTime.now())) {
            throw new TripValidationException("A saída prevista da origem não pode estar no passado.");
        }
        if (!dto.plannedArrivalDestination().isAfter(dto.plannedDepartureOrigin())) {
            throw new TripValidationException("A chegada prevista ao destino deve ser posterior à saída da origem.");
        }
        if (!dto.plannedDepartureDestination().isAfter(dto.plannedArrivalDestination())) {
            throw new TripValidationException("A saída prevista do destino deve ser posterior à chegada ao destino.");
        }
        if (!dto.plannedArrivalOrigin().isAfter(dto.plannedDepartureDestination())) {
            throw new TripValidationException("A chegada prevista à origem deve ser posterior à saída do destino.");
        }
    }

    private void validateVehicle(Vehicle vehicle) {
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new TripValidationException("O ônibus " + vehicle.getPrefix() + " está " + vehicle.getStatus().getLabel().toLowerCase() + " e não pode ser agendado.");
        }
    }

    private void validateDrivers(Set<Collaborator> drivers) {
        for (Collaborator driver : drivers) {
            if (driver.getPosition() != Position.DRIVER) {
                throw new TripValidationException(driver.getName() + " não é motorista e não pode ser escalado.");
            }
            if (!driver.isActive()) {
                throw new TripValidationException("O motorista " + driver.getName() + " está inativo.");
            }
            if (driver.getCnh() == null || driver.getCnh().getNumber() == null
                    || driver.getCnh().getNumber().isBlank()) {
                throw new TripValidationException("O motorista " + driver.getName() + " não tem CNH cadastrada.");
            }
        }
    }
    
    private void validateScheduleConflicts(TripRequestDTO dto, Vehicle vehicle, Set<Collaborator> drivers, Long excludeTripId) {
        LocalDateTime start = dto.plannedDepartureOrigin();
        LocalDateTime end = dto.plannedArrivalOrigin();

        List<Long> vehicleConflicts = tripRepository.findVehicleOverlapIds(vehicle.getId(), start, end, excludeTripId);
        if (!vehicleConflicts.isEmpty()) {
            throw new TripValidationException("O ônibus " + vehicle.getPrefix() + " já está escalado na viagem " + vehicleConflicts.get(0) + " no mesmo período.");
        }

        for (Collaborator driver : drivers) {
            List<Long> driverConflicts = tripRepository.findDriverOverlapIds(driver.getId(), start, end, excludeTripId);
            if (!driverConflicts.isEmpty()) {
                throw new TripValidationException("O motorista " + driver.getName() + " já está escalado na viagem " + driverConflicts.get(0) + " no mesmo período.");
            }
        }
    }

    private void replacePassengersIfProvided(Trip trip, List<TripPassengerRequestDTO> requests) {
        if (requests == null) {
            return;
        }
        if (requests.size() > trip.getVehicle().getSeats()) {
            throw new TripValidationException("A viagem comporta no máximo "
                    + trip.getVehicle().getSeats() + " passageiros.");
        }

        Set<Long> passengerIds = new HashSet<>();
        List<TripPassenger> passengers = new ArrayList<>();
        for (TripPassengerRequestDTO request : requests) {
            if (request.passengerId() == null || !passengerIds.add(request.passengerId())) {
                throw new TripValidationException("Não é permitido incluir o mesmo passageiro duas vezes na viagem.");
            }
            BigDecimal amountDue = request.amountDue() != null
                    ? request.amountDue() : trip.getEstimatedPassengerAmount();
            BigDecimal amountPaid = request.amountPaid() == null ? BigDecimal.ZERO : request.amountPaid();
            validateAmounts(amountDue, amountPaid);
            Passenger passenger = passengerRepository.findById(request.passengerId())
                    .orElseThrow(() -> new PassengerNotFoundException(request.passengerId()));
            passengers.add(new TripPassenger(null, passenger, amountDue, amountPaid));
        }
        trip.replacePassengers(passengers);
    }

    private void validateAmounts(BigDecimal amountDue, BigDecimal amountPaid) {
        if (amountPaid.signum() < 0) {
            throw new TripValidationException("O valor pago não pode ser negativo.");
        }
        if (amountDue != null && amountDue.signum() < 0) {
            throw new TripValidationException("O valor devido não pode ser negativo.");
        }
        if (amountDue != null && amountPaid.compareTo(amountDue) > 0) {
            throw new TripValidationException("O valor pago não pode ultrapassar o valor devido do passageiro.");
        }
    }

    private Passenger resolveResponsible(Long responsiblePassengerId, List<TripPassenger> passengers) {
        if (responsiblePassengerId == null) {
            return null;
        }
        boolean included = passengers.stream()
                .anyMatch(passenger -> passenger.getPassenger().getId().equals(responsiblePassengerId));
        if (!included) {
            throw new TripValidationException("O responsável deve estar incluído na lista de passageiros da viagem.");
        }
        return passengers.stream()
                .map(TripPassenger::getPassenger)
                .filter(passenger -> passenger.getId().equals(responsiblePassengerId))
                .findFirst()
                .orElseThrow();
    }

    private Set<br.com.marazulturismo.marazulbackendadmin.model.Collaborator> findDrivers(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<br.com.marazulturismo.marazulbackendadmin.model.Collaborator> drivers = collaboratorRepository.findAllById(ids);

        if(drivers.size() < ids.size()) {
            Set<Long> foundIds = new HashSet<>();
            drivers.forEach(driver -> foundIds.add(driver.getId()));
            Long missingId = ids.stream().filter(id -> !foundIds.contains(id)).findFirst().orElseThrow();
            throw new EmployeeNotFoundException(missingId);
        }
        return new HashSet<>(drivers);

    }

    private br.com.marazulturismo.marazulbackendadmin.model.City findCity(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new CityNotFoundException(id));
    }

    private Vehicle findVehicle(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new VehicleNotFoundException(id));
    }

    private br.com.marazulturismo.marazulbackendadmin.model.Client findClient(Long id) {
        return id == null ? null : clientRepository.findById(id).orElseThrow(() -> new ClientNotFoundException(id));
    }

    private Trip findEntity(Long id) {
        return tripRepository.findById(id).orElseThrow(() -> new TripNotFoundException(id));
    }
}