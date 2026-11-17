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
        String originStateAcronym,
        Long destinationCityId,
        String destinationCity,
        String destinationStateAcronym,
        LocalDateTime plannedDepartureOrigin,
        LocalDateTime departureOrigin,
        LocalDateTime plannedArrivalDestination,
        LocalDateTime arrivalDestination,
        LocalDateTime plannedDepartureDestination,
        LocalDateTime departureDestination,
        LocalDateTime plannedArrivalOrigin,
        LocalDateTime arrivalOrigin,
        Long vehicleId,
        String vehiclePrefix,
        String vehicleLicensePlate,
        Integer vehicleSeats,
        TripStatus status,
        List<TripDriverResponseDTO> drivers,
        Long clientId,
        String clientName,
        Integer minimumPassengers,
        BigDecimal estimatedPassengerAmount,
        BigDecimal estimatedTotalValue,
        BigDecimal estimatedIndividualValue,
        String notes,
        Long responsiblePassengerId,
        String responsiblePassengerName,
        Integer passengerCount,
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
        List<TripDriverResponseDTO> drivers = trip.getDrivers().stream()
                .map(TripDriverResponseDTO::fromEntity)
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
                trip.getOriginCity().getState().getAcronym(),
                trip.getDestinationCity().getId(),
                trip.getDestinationCity().getName(),
                trip.getDestinationCity().getState().getAcronym(),
                trip.getPlannedDepartureOrigin(),
                trip.getDepartureOrigin(),
                trip.getPlannedArrivalDestination(),
                trip.getArrivalDestination(),
                trip.getPlannedDepartureDestination(),
                trip.getDepartureDestination(),
                trip.getPlannedArrivalOrigin(),
                trip.getArrivalOrigin(),
                trip.getVehicle().getId(),
                trip.getVehicle().getPrefix(),
                trip.getVehicle().getLicensePlate(),
                trip.getVehicle().getSeats(),
                trip.getStatus(),
                drivers,
                trip.getClient() == null ? null : trip.getClient().getId(),
                trip.getClient() == null ? null : trip.getClient().getName(),
                trip.getMinimumPassengers(),
                trip.getEstimatedPassengerAmount(),
                estimatedTotalValue,
                estimatedIndividualValue,
                trip.getNotes(),
                trip.getResponsiblePassenger() == null ? null : trip.getResponsiblePassenger().getId(),
                trip.getResponsiblePassenger() == null ? null : trip.getResponsiblePassenger().getName(),
                passengers.size(),
                passengers,
                expenses,
                totalExpenses,
                totalReceived,
                totalOpen);
    }
}
