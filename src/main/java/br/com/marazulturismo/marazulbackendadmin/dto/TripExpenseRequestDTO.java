package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TripExpenseRequestDTO(
        @NotBlank(message = "A descrição do gasto é obrigatória.")
        String description,
        @NotNull(message = "O valor do gasto é obrigatório.")
        @Positive(message = "O valor do gasto deve ser maior que zero.")
        BigDecimal amount
) {
}