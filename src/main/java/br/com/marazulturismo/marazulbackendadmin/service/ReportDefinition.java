package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportColumnDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportContentDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;

import java.util.List;

public interface ReportDefinition {

    String slug();

    String title();

    List<ReportColumnDTO> columns();

    ReportContentDTO content(ReportPeriodDTO period);

    default boolean requiresPeriod() {
        return true;
    }
}
