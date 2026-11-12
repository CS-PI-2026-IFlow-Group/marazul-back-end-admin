package br.com.marazulturismo.marazulbackendadmin.dto;

public record ReportSummaryDTO(
        String relatorio,
        String titulo,
        boolean exigePeriodo
) {
}
