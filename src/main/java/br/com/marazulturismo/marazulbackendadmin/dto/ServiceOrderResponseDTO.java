package br.com.marazulturismo.marazulbackendadmin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ServiceOrderResponseDTO(
        Long tripId,
        String originCity,
        String originState,
        String destinationCity,
        String destinationState,
        LocalDateTime plannedDepartureOrigin,
        LocalDateTime plannedArrivalDestination,
        LocalDateTime plannedDepartureDestination,
        LocalDateTime plannedArrivalOrigin,
        String vehiclePrefix,
        String vehicleLicensePlate,
        List<String> drivers,
        String clientName,
        String responsiblePassengerName,
        String responsiblePassengerPhone,
        String notes
) {
}
