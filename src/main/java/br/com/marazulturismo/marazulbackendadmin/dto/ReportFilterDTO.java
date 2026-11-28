package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.ReportFilterType;

public record ReportFilterDTO(
        String chave,
        String titulo,
        ReportFilterType tipo
) {
}
