package br.com.marazulturismo.marazulbackendadmin.dto;

import java.util.List;
import java.util.Map;

public record ReportContentDTO(
        List<Map<String, Object>> rows,
        List<ReportTotalDTO> totals
) {
    public static ReportContentDTO of(List<Map<String, Object>> rows) {
        return new ReportContentDTO(rows, List.of());
    }

    public static ReportContentDTO of(List<Map<String, Object>> rows, List<ReportTotalDTO> totals) {
        return new ReportContentDTO(rows, totals);
    }

    public static ReportContentDTO empty() {
        return new ReportContentDTO(List.of(), List.of());
    }

    public List<Map<String, Object>> rows() {
        return rows == null ? List.of() : rows;
    }

    public List<ReportTotalDTO> totals() {
        return totals == null ? List.of() : totals;
    }
}
