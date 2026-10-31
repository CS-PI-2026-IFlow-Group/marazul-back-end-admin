package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.TripPassengerRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.ClientNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.PassengerNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.TripNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.TripValidationException;
import br.com.marazulturismo.marazulbackendadmin.exception.VehicleNotFoundException;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TripService {

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
        Vehicle vehicle = findVehicle(dto.vehicleId());
        Trip trip = new Trip(
                findCity(dto.originCityId()),
                findCity(dto.destinationCityId()),
                dto.plannedDepartureOrigin(),
                dto.plannedArrivalDestination(),
                dto.plannedDepartureDestination(),
                dto.plannedArrivalOrigin(),
                vehicle,
                findDrivers(dto.driverIds()),
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
        Vehicle vehicle = findVehicle(dto.vehicleId());
        trip.update(
                findCity(dto.originCityId()),
                findCity(dto.destinationCityId()),
                dto.plannedDepartureOrigin(),
                dto.plannedArrivalDestination(),
                dto.plannedDepartureDestination(),
                dto.plannedArrivalOrigin(),
                vehicle,
                findDrivers(dto.driverIds()),
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
        return new HashSet<>(collaboratorRepository.findAllById(ids));
    }

    private br.com.marazulturismo.marazulbackendadmin.model.City findCity(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new TripValidationException("Cidade não encontrada: " + id));
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