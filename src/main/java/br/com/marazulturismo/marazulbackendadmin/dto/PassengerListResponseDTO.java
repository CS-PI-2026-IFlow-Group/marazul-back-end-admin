package br.com.marazulturismo.marazulbackendadmin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PassengerListResponseDTO(
        Long tripId,
        String originCity,
        String originState,
        String destinationCity,
        String destinationState,
        LocalDateTime plannedDepartureOrigin,
        String vehiclePrefix,
        String vehicleLicensePlate,
        List<String> drivers,
        PassengerListItemDTO responsible,
        List<PassengerListItemDTO> passengers,
        int totalPassengers,
        LocalDateTime issuedAt
) {
}
