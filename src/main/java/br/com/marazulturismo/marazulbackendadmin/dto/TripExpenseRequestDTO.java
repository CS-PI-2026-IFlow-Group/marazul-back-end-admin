package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TripExpenseRequestDTO(
        @NotBlank(message = "A descrição do gasto é obrigatória.")
        @Size(max = 255, message = "A descrição do gasto deve ter no máximo 255 caracteres.")
        String description,
        @NotNull(message = "O valor do gasto é obrigatório.")
        @Positive(message = "O valor do gasto deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "O valor do gasto deve ter no máximo 10 dígitos e 2 casas decimais.")
        BigDecimal amount
) {
}