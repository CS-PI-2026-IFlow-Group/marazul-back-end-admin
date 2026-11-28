package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Expense;

import java.math.BigDecimal;

public record TripExpenseResponseDTO(Long id, String description, BigDecimal amount) {
    public static TripExpenseResponseDTO fromEntity(Expense expense) {
        return new TripExpenseResponseDTO(expense.getId(), expense.getDescription(), expense.getAmount());
    }
}