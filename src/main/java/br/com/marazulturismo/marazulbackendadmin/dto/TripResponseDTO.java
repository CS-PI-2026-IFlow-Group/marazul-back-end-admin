package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TripResponseDTO(
        Long id,
        Long originCityId,
        String originCity,
        Long destinationCityId,
        String destinationCity,
        LocalDateTime plannedDepartureOrigin,
        LocalDateTime plannedArrivalDestination,
        LocalDateTime plannedDepartureDestination,
        LocalDateTime plannedArrivalOrigin,
        Long vehicleId,
        Integer vehicleSeats,
        TripStatus status,
        Integer minimumPassengers,
        BigDecimal estimatedPassengerAmount,
        String notes,
        Long responsiblePassengerId,
        List<TripPassengerResponseDTO> passengers,
        BigDecimal totalReceived,
        BigDecimal totalOpen
) {
    public static TripResponseDTO fromEntity(Trip trip) {
        List<TripPassengerResponseDTO> passengers = trip.getPassengers().stream()
                .map(TripPassengerResponseDTO::fromEntity)
                .toList();
        BigDecimal totalReceived = passengers.stream()
                .map(TripPassengerResponseDTO::amountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOpen = passengers.stream()
                .filter(passenger -> passenger.amountDue() != null)
                .map(passenger -> passenger.amountDue().subtract(passenger.amountPaid()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TripResponseDTO(
                trip.getId(),
                trip.getOriginCity().getId(),
                trip.getOriginCity().getName(),
                trip.getDestinationCity().getId(),
                trip.getDestinationCity().getName(),
                trip.getPlannedDepartureOrigin(),
                trip.getPlannedArrivalDestination(),
                trip.getPlannedDepartureDestination(),
                trip.getPlannedArrivalOrigin(),
                trip.getVehicle().getId(),
                trip.getVehicle().getSeats(),
                trip.getStatus(),
                trip.getMinimumPassengers(),
                trip.getEstimatedPassengerAmount(),
                trip.getNotes(),
                trip.getResponsiblePassenger() == null ? null : trip.getResponsiblePassenger().getId(),
                passengers,
                totalReceived,
                totalOpen);
    }
}