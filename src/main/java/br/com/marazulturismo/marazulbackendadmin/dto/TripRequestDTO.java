package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.Valid;
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
        @DecimalMin(value = "0.00", message = "O valor estimado total não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "O valor estimado total deve ter no máximo 10 dígitos e 2 casas decimais.")
        BigDecimal estimatedTotalValue,
        @DecimalMin(value = "0.00", message = "O valor estimado por passageiro não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "O valor estimado por passageiro deve ter no máximo 10 dígitos e 2 casas decimais.")
        BigDecimal estimatedIndividualValue,
        String notes,
        List<TripPassengerRequestDTO> passengers,
        Long responsiblePassengerId,
        List<@Valid TripExpenseRequestDTO> expenses
) {
    public TripRequestDTO(Long originCityId, Long destinationCityId,
                          LocalDateTime plannedDepartureOrigin, LocalDateTime plannedArrivalDestination,
                          LocalDateTime plannedDepartureDestination, LocalDateTime plannedArrivalOrigin,
                          Long vehicleId, Set<Long> driverIds, Long clientId, Integer minimumPassengers,
                          BigDecimal estimatedPassengerAmount, String notes,
                          List<TripPassengerRequestDTO> passengers, Long responsiblePassengerId) {
        this(originCityId, destinationCityId, plannedDepartureOrigin, plannedArrivalDestination,
                plannedDepartureDestination, plannedArrivalOrigin, vehicleId, driverIds, clientId,
                minimumPassengers, estimatedPassengerAmount, null, null, notes, passengers,
                responsiblePassengerId, null);
    }
}