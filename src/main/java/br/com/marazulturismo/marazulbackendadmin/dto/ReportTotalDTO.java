package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;

public record ReportTotalDTO(
        String chave,
        String titulo,
        ReportValueFormat formato,
        Object valor
) {
    public ReportTotalDTO(String chave, String titulo, Object valor) {
        this(chave, titulo, ReportValueFormat.TEXTO, valor);
    }
}
