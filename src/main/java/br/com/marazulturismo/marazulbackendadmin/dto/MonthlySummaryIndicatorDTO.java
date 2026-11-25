package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;

import java.math.BigDecimal;

public record MonthlySummaryIndicatorDTO(
        String chave,
        String titulo,
        ReportValueFormat formato,
        BigDecimal valor,
        BigDecimal valorAnterior,
        BigDecimal variacao,
        BigDecimal variacaoPercentual
) {
}
