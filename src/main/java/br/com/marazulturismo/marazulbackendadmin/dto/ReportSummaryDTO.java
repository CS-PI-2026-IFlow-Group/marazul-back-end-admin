package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.ReportDefaultPeriod;

public record ReportSummaryDTO(
        String relatorio,
        String titulo,
        boolean exigePeriodo,
        ReportDefaultPeriod periodoPadrao
) {
    public ReportSummaryDTO(String relatorio, String titulo, boolean exigePeriodo) {
        this(relatorio, titulo, exigePeriodo, ReportDefaultPeriod.MES);
    }
}
