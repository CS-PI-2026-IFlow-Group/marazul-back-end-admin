package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TripExpensesRequestDTO(
        @NotNull(message = "A lista de gastos é obrigatória.")
        List<@NotNull(message = "O item de gasto não pode ser nulo.") @Valid TripExpenseRequestDTO> expenses
) {
}
