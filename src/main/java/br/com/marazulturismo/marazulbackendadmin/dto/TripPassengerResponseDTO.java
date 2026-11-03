package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;

import java.math.BigDecimal;

public record TripPassengerResponseDTO(
        Long passengerId,
        String name,
        String cpf,
        BigDecimal amountDue,
        BigDecimal amountPaid
) {
    public static TripPassengerResponseDTO fromEntity(TripPassenger tripPassenger) {
        return new TripPassengerResponseDTO(
                tripPassenger.getPassenger().getId(),
                tripPassenger.getPassenger().getName(),
                tripPassenger.getPassenger().getCpf(),
                tripPassenger.getAmountDue(),
                tripPassenger.getAmountPaid());
    }
}