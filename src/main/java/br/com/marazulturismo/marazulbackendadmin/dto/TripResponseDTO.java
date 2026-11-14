package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        BigDecimal estimatedTotalValue,
        BigDecimal estimatedIndividualValue,
        String notes,
        Long responsiblePassengerId,
        List<TripPassengerResponseDTO> passengers,
        List<TripExpenseResponseDTO> expenses,
        BigDecimal totalExpenses,
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
        List<TripExpenseResponseDTO> expenses = trip.getExpenses().stream()
                .map(TripExpenseResponseDTO::fromEntity)
                .toList();
        BigDecimal totalExpenses = expenses.stream()
                .map(TripExpenseResponseDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal estimatedTotalValue = trip.getEstimatedTotalValue() == null
                ? totalExpenses : trip.getEstimatedTotalValue();
        int passengerCount = passengers.isEmpty()
                ? (trip.getMinimumPassengers() == null ? 0 : trip.getMinimumPassengers())
                : passengers.size();
        BigDecimal calculatedIndividualValue = passengerCount > 0
                ? estimatedTotalValue.divide(BigDecimal.valueOf(passengerCount), 2, RoundingMode.HALF_UP)
                : null;
        BigDecimal estimatedIndividualValue = trip.getEstimatedIndividualValue() == null
                ? calculatedIndividualValue : trip.getEstimatedIndividualValue();

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
                estimatedTotalValue,
                estimatedIndividualValue,
                trip.getNotes(),
                trip.getResponsiblePassenger() == null ? null : trip.getResponsiblePassenger().getId(),
                passengers,
                expenses,
                totalExpenses,
                totalReceived,
                totalOpen);
    }
}