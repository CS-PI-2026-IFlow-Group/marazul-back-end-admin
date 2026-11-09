package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties({"departureOrigin", "arrivalDestination", "departureDestination", "arrivalOrigin"})
public record TripRequestDTO(

        @NotNull(message = "A cidade de origem é obrigatória.") 
        Long originCityId,

        @NotNull(message = "A cidade de destino é obrigatória.") 
        Long destinationCityId,

        @NotNull(message = "A saída prevista da origem é obrigatória.") 
        LocalDateTime plannedDepartureOrigin,

        @NotNull(message = "A chegada prevista ao destino é obrigatória.") 
        LocalDateTime plannedArrivalDestination,

        @NotNull(message = "A saída prevista do destino é obrigatória.")
        LocalDateTime plannedDepartureDestination,

        @NotNull(message = "A chegada prevista à origem é obrigatória.") 
        LocalDateTime plannedArrivalOrigin,

        @NotNull(message = "O ônibus é obrigatório.") 
        Long vehicleId,

        @NotEmpty(message = "Informe ao menos um motorista.")
        Set<Long> driverIds,

        Long clientId,
        Integer minimumPassengers,
        BigDecimal estimatedPassengerAmount,
        String notes,
        List<TripPassengerRequestDTO> passengers,
        Long responsiblePassengerId
) {
}