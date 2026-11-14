package br.com.marazulturismo.marazulbackendadmin.dto;

import java.time.LocalDateTime;

import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;
import jakarta.validation.constraints.NotNull;

public record TripStatusRequestDTO(

    @NotNull(message = "A situação é obrigatória.")
    TripStatus status,

    LocalDateTime dataHora

) {
}
