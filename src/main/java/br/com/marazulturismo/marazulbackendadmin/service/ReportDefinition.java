package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportColumnDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportContentDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportFilterDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportDefaultPeriod;

import java.util.List;
import java.util.Map;

public interface ReportDefinition {

    String slug();

    String title();

    List<ReportColumnDTO> columns();

    ReportContentDTO content(ReportPeriodDTO period);

    default ReportContentDTO content(ReportPeriodDTO period, Map<String, String> filters) {
        return content(period);
    }

    default List<ReportFilterDTO> filters() {
        return List.of();
    }

    default boolean requiresPeriod() {
        return true;
    }

    default ReportDefaultPeriod defaultPeriod() {
        return ReportDefaultPeriod.MES;
    }
}
