package br.com.marazulturismo.marazulbackendadmin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MonthlySummaryResponseDTO(
        String mes,
        ReportPeriodDTO periodo,
        LocalDateTime emitidoEm,
        List<MonthlySummaryIndicatorDTO> indicadores,
        List<MonthlyTopClientDTO> principaisClientes
) {
}
