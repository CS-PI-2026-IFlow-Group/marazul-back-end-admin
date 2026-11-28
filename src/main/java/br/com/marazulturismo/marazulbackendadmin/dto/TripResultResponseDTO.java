package br.com.marazulturismo.marazulbackendadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TripResultResponseDTO(
        Long viagemId,
        String trecho,
        LocalDateTime dataSaida,
        String cliente,
        String situacao,
        BigDecimal cobrado,
        BigDecimal recebido,
        BigDecimal gastos,
        BigDecimal resultado,
        List<TripResultExpenseDTO> gastosDetalhados,
        LocalDateTime emitidoEm
) {
}
