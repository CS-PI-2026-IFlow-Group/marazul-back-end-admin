package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;

import java.time.LocalDate;

public record VehicleResponseDTO(
        Long id,
        String prefix,
        String licensePlate,
        BodyworkModel model,
        VehicleType type,
        Integer year,
        Integer seats,
        LocalDate inspectionDate,
        VehicleStatus status
) {
    public static VehicleResponseDTO fromEntity(Vehicle vehicle) {
        return new VehicleResponseDTO(
                vehicle.getId(),
                vehicle.getPrefix(),
                vehicle.getLicensePlate(),
                vehicle.getModel(),
                vehicle.getType(),
                vehicle.getYear(),
                vehicle.getSeats(),
                vehicle.getInspectionDate(),
                vehicle.getStatus()
        );
    }
}
