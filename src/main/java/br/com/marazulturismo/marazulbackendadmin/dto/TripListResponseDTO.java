package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;

import java.time.LocalDateTime;

public record TripListResponseDTO(
        Long id,
        Long originCityId,
        String originCity,
        String originStateAcronym,
        Long destinationCityId,
        String destinationCity,
        String destinationStateAcronym,
        LocalDateTime plannedDepartureOrigin,
        LocalDateTime plannedArrivalDestination,
        Long vehicleId,
        String vehiclePrefix,
        String vehicleLicensePlate,
        Integer passengerCount,
        TripStatus status,
        Long clientId,
        String clientName
) {
    public static TripListResponseDTO fromEntity(Trip trip) {
        return new TripListResponseDTO(
                trip.getId(),
                trip.getOriginCity().getId(),
                trip.getOriginCity().getName(),
                trip.getOriginCity().getState().getAcronym(),
                trip.getDestinationCity().getId(),
                trip.getDestinationCity().getName(),
                trip.getDestinationCity().getState().getAcronym(),
                trip.getPlannedDepartureOrigin(),
                trip.getPlannedArrivalDestination(),
                trip.getVehicle().getId(),
                trip.getVehicle().getPrefix(),
                trip.getVehicle().getLicensePlate(),
                trip.getPassengers().size(),
                trip.getStatus(),
                trip.getClient() == null ? null : trip.getClient().getId(),
                trip.getClient() == null ? null : trip.getClient().getName());
    }
}
