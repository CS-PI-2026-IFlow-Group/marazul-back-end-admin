package br.com.marazulturismo.marazulbackendadmin.dto;

import java.math.BigDecimal;

public record TripResultExpenseDTO(
        String descricao,
        BigDecimal valor
) {
}
