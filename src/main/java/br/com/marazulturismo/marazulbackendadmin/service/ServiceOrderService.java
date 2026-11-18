package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ServiceOrderResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.TripNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.List;
import java.util.Locale;

@Service
public class ServiceOrderService {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    private final TripRepository tripRepository;

    public ServiceOrderService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public ServiceOrderResponseDTO generate(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        Passenger responsible = trip.getResponsiblePassenger();
        List<String> drivers = trip.getDrivers().stream()
                .map(Collaborator::getName)
                .sorted(Collator.getInstance(BRAZIL))
                .toList();

        return new ServiceOrderResponseDTO(
                trip.getId(),
                trip.getOriginCity().getName(),
                trip.getOriginCity().getState().getAcronym(),
                trip.getDestinationCity().getName(),
                trip.getDestinationCity().getState().getAcronym(),
                trip.getPlannedDepartureOrigin(),
                trip.getPlannedArrivalDestination(),
                trip.getPlannedDepartureDestination(),
                trip.getPlannedArrivalOrigin(),
                trip.getVehicle().getPrefix(),
                trip.getVehicle().getLicensePlate(),
                drivers,
                trip.getClient() == null ? null : trip.getClient().getName(),
                responsible == null ? null : responsible.getName(),
                responsible == null ? null : responsible.getPhone(),
                trip.getNotes());
    }
}
