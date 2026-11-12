package br.com.marazulturismo.marazulbackendadmin.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ReportTableResponseDTO(
        String relatorio,
        String titulo,
        ReportPeriodDTO periodo,
        LocalDateTime emitidoEm,
        List<ReportColumnDTO> colunas,
        List<Map<String, Object>> linhas,
        List<ReportTotalDTO> totais
) {
}
