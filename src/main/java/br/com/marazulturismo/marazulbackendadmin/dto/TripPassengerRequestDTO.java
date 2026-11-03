package br.com.marazulturismo.marazulbackendadmin.dto;

import java.math.BigDecimal;

public record TripPassengerRequestDTO(
        Long passengerId,
        BigDecimal amountDue,
        BigDecimal amountPaid
) {
}