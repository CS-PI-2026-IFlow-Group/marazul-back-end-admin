package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.ReportColumnAlignment;

public record ReportColumnDTO(
        String chave,
        String titulo,
        ReportColumnAlignment alinhamento
) {
    public static ReportColumnDTO text(String chave, String titulo) {
        return new ReportColumnDTO(chave, titulo, ReportColumnAlignment.LEFT);
    }

    public static ReportColumnDTO number(String chave, String titulo) {
        return new ReportColumnDTO(chave, titulo, ReportColumnAlignment.RIGHT);
    }

    public static ReportColumnDTO centered(String chave, String titulo) {
        return new ReportColumnDTO(chave, titulo, ReportColumnAlignment.CENTER);
    }
}
