package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record TripRequestDTO(
        @NotNull Long originCityId,
        @NotNull Long destinationCityId,
        @NotNull LocalDateTime plannedDepartureOrigin,
        @NotNull LocalDateTime plannedArrivalDestination,
        @NotNull LocalDateTime plannedDepartureDestination,
        @NotNull LocalDateTime plannedArrivalOrigin,
        @NotNull Long vehicleId,
        Set<Long> driverIds,
        Long clientId,
        Integer minimumPassengers,
        BigDecimal estimatedPassengerAmount,
        String notes,
        List<TripPassengerRequestDTO> passengers,
        Long responsiblePassengerId
) {
}