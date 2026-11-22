package br.com.marazulturismo.marazulbackendadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PassengerReceiptResponseDTO(
        String receiptNumber,
        Long tripId,
        Long passengerId,
        String passengerName,
        String passengerCpf,
        BigDecimal amountPaid,
        String originCity,
        String originStateAcronym,
        String destinationCity,
        String destinationStateAcronym,
        LocalDateTime plannedDepartureOrigin,
        String companyName,
        LocalDateTime issuedAt
) {
}
