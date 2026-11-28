package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.ReportDefaultPeriod;

import java.util.List;

public record ReportSummaryDTO(
        String relatorio,
        String titulo,
        boolean exigePeriodo,
        ReportDefaultPeriod periodoPadrao,
        List<ReportFilterDTO> filtros
) {
    public ReportSummaryDTO(String relatorio, String titulo, boolean exigePeriodo) {
        this(relatorio, titulo, exigePeriodo, ReportDefaultPeriod.MES, List.of());
    }
}
