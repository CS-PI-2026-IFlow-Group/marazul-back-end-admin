package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerListItemDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.PassengerListResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.TripNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.TripValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class PassengerListService {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    private final TripRepository tripRepository;

    public PassengerListService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public PassengerListResponseDTO generate(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new TripNotFoundException(tripId));
        if (trip.getPassengers().isEmpty()) {
            throw new TripValidationException("A viagem " + tripId
                    + " não tem passageiros vinculados, por isso a lista de passageiros não pode ser emitida.");
        }

        Collator collator = Collator.getInstance(BRAZIL);
        Long responsibleId = trip.getResponsiblePassenger() == null ? null : trip.getResponsiblePassenger().getId();

        List<Passenger> passengers = trip.getPassengers().stream()
                .map(TripPassenger::getPassenger)
                .sorted(Comparator.comparing(Passenger::getName, collator))
                .toList();

        List<PassengerListItemDTO> items = new ArrayList<>();
        for (int i = 0; i < passengers.size(); i++) {
            Passenger passenger = passengers.get(i);
            items.add(new PassengerListItemDTO(
                    i + 1,
                    passenger.getId(),
                    passenger.getName(),
                    passenger.getCpf(),
                    passenger.getPhone(),
                    Objects.equals(passenger.getId(), responsibleId)));
        }

        PassengerListItemDTO responsible = items.stream()
                .filter(PassengerListItemDTO::responsible)
                .findFirst()
                .orElse(null);

        List<String> drivers = trip.getDrivers().stream()
                .map(Collaborator::getName)
                .sorted(collator)
                .toList();

        return new PassengerListResponseDTO(
                trip.getId(),
                trip.getOriginCity().getName(),
                trip.getOriginCity().getState().getAcronym(),
                trip.getDestinationCity().getName(),
                trip.getDestinationCity().getState().getAcronym(),
                trip.getPlannedDepartureOrigin(),
                trip.getVehicle().getPrefix(),
                trip.getVehicle().getLicensePlate(),
                drivers,
                responsible,
                items,
                items.size(),
                LocalDateTime.now());
    }
}
