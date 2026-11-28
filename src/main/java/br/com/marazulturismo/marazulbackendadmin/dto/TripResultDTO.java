package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record TripResultDTO(
        Long tripId,
        LocalDateTime plannedDepartureOrigin,
        String originCity,
        String originState,
        String destinationCity,
        String destinationState,
        String client,
        TripStatus status,
        BigDecimal charged,
        BigDecimal received,
        BigDecimal expenses
) {
    public TripResultDTO {
        charged = charged == null ? null : money(charged);
        received = received == null ? money(BigDecimal.ZERO) : money(received);
        expenses = expenses == null ? money(BigDecimal.ZERO) : money(expenses);
    }

    public String route() {
        return originCity + "/" + originState + " – " + destinationCity + "/" + destinationState;
    }

    public BigDecimal result() {
        return received.subtract(expenses);
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
