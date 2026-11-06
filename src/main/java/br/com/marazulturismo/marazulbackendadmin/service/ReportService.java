package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportContentDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportSummaryDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTableResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.ReportNotFoundException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final Map<String, ReportDefinition> definitions;

    public ReportService(ObjectProvider<ReportDefinition> definitions) {
        this.definitions = definitions.orderedStream()
                .sorted(Comparator.comparing(ReportDefinition::slug))
                .collect(LinkedHashMap::new, (map, it) -> map.put(it.slug(), it), Map::putAll);
    }

    public List<ReportSummaryDTO> available() {
        return definitions.values().stream()
                .map(it -> new ReportSummaryDTO(it.slug(), it.title(), it.requiresPeriod()))
                .toList();
    }

    public ReportTableResponseDTO generate(String slug, LocalDate startDate, LocalDate endDate) {
        ReportDefinition definition = resolve(slug);
        ReportPeriodDTO period = period(definition, startDate, endDate);
        ReportContentDTO content = definition.content(period);

        return new ReportTableResponseDTO(
                definition.slug(),
                definition.title(),
                period,
                LocalDateTime.now(),
                definition.columns(),
                content.rows(),
                content.totals());
    }

    private ReportDefinition resolve(String slug) {
        ReportDefinition definition = definitions.get(slug);

        if (definition == null) {
            throw new ReportNotFoundException("Relatório não encontrado: " + slug + ".");
        }

        return definition;
    }

    private static ReportPeriodDTO period(ReportDefinition definition, LocalDate startDate, LocalDate endDate) {
        if (!definition.requiresPeriod() && startDate == null && endDate == null) {
            return null;
        }

        return ReportPeriodDTO.of(startDate, endDate);
    }
}
