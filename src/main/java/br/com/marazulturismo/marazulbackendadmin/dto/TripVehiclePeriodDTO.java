package br.com.marazulturismo.marazulbackendadmin.dto;

import java.time.LocalDateTime;

public record TripVehiclePeriodDTO(
        Long vehicleId,
        LocalDateTime plannedDepartureOrigin,
        LocalDateTime plannedArrivalOrigin
) {
}
