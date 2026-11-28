package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.ReportColumnAlignment;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;

public record ReportColumnDTO(
        String chave,
        String titulo,
        ReportColumnAlignment alinhamento,
        ReportValueFormat formato
) {
    public ReportColumnDTO(String chave, String titulo, ReportColumnAlignment alinhamento) {
        this(chave, titulo, alinhamento, ReportValueFormat.TEXTO);
    }

    public static ReportColumnDTO text(String chave, String titulo) {
        return new ReportColumnDTO(chave, titulo, ReportColumnAlignment.LEFT, ReportValueFormat.TEXTO);
    }

    public static ReportColumnDTO number(String chave, String titulo) {
        return new ReportColumnDTO(chave, titulo, ReportColumnAlignment.RIGHT, ReportValueFormat.NUMERO);
    }

    public static ReportColumnDTO centered(String chave, String titulo) {
        return new ReportColumnDTO(chave, titulo, ReportColumnAlignment.CENTER, ReportValueFormat.TEXTO);
    }

    public static ReportColumnDTO money(String chave, String titulo) {
        return new ReportColumnDTO(chave, titulo, ReportColumnAlignment.RIGHT, ReportValueFormat.MOEDA);
    }

    public static ReportColumnDTO percent(String chave, String titulo) {
        return new ReportColumnDTO(chave, titulo, ReportColumnAlignment.RIGHT, ReportValueFormat.PERCENTUAL);
    }

    public static ReportColumnDTO date(String chave, String titulo) {
        return new ReportColumnDTO(chave, titulo, ReportColumnAlignment.CENTER, ReportValueFormat.DATA);
    }
}
