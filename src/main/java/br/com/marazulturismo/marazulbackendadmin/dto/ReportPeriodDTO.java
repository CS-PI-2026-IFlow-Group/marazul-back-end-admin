package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.exception.ReportPeriodValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportPeriodDTO(
        LocalDate dataInicio,
        LocalDate dataFim
) {
    public static ReportPeriodDTO of(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new ReportPeriodValidationException(
                    "Informe o período do relatório: dataInicio e dataFim são obrigatórias.");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new ReportPeriodValidationException(
                    "A data inicial do período não pode ser maior que a data final.");
        }
        return new ReportPeriodDTO(dataInicio, dataFim);
    }

    public LocalDateTime start() {
        return dataInicio.atStartOfDay();
    }

    public LocalDateTime exclusiveEnd() {
        return dataFim.plusDays(1).atStartOfDay();
    }
}
