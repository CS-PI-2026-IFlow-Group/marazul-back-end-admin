package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportColumnDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportContentDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportFilterDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTotalDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripResultDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportFilterType;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;
import br.com.marazulturismo.marazulbackendadmin.exception.ClientNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.ReportFilterValidationException;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripResultRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Component
public class TripResultsReport implements ReportDefinition {

    static final String CLIENT_FILTER = "clienteId";

    private final TripResultRepository tripResultRepository;
    private final ClientRepository clientRepository;

    public TripResultsReport(TripResultRepository tripResultRepository, ClientRepository clientRepository) {
        this.tripResultRepository = tripResultRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public String slug() {
        return "resultado-viagens";
    }

    @Override
    public String title() {
        return "Resultado das viagens";
    }

    @Override
    public List<ReportColumnDTO> columns() {
        return List.of(
                ReportColumnDTO.date("data", "Data"),
                ReportColumnDTO.text("trecho", "Trecho"),
                ReportColumnDTO.text("cliente", "Cliente"),
                ReportColumnDTO.centered("situacao", "Situação"),
                ReportColumnDTO.money("cobrado", "Cobrado"),
                ReportColumnDTO.money("recebido", "Recebido"),
                ReportColumnDTO.money("gastos", "Gastos"),
                ReportColumnDTO.money("resultado", "Resultado"));
    }

    @Override
    public List<ReportFilterDTO> filters() {
        return List.of(new ReportFilterDTO(CLIENT_FILTER, "Cliente", ReportFilterType.CLIENTE));
    }

    @Override
    public ReportContentDTO content(ReportPeriodDTO period) {
        return content(period, Map.of());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportContentDTO content(ReportPeriodDTO period, Map<String, String> filters) {
        Long clientId = clientId(filters);
        if (clientId != null && !clientRepository.existsById(clientId)) {
            throw new ClientNotFoundException(clientId);
        }

        List<TripResultDTO> results = tripResultRepository.findResultsBetween(
                period.start(), period.exclusiveEnd(), clientId);

        List<Map<String, Object>> rows = results.stream().map(TripResultsReport::row).toList();

        BigDecimal received = sum(results, TripResultDTO::received);
        BigDecimal expenses = sum(results, TripResultDTO::expenses);
        List<ReportTotalDTO> totals = List.of(
                money("cobrado", "Cobrado", sum(results, TripResultDTO::charged)),
                money("recebido", "Recebido", received),
                money("gastos", "Gastos", expenses),
                money("resultado", "Resultado", received.subtract(expenses)));

        return ReportContentDTO.of(rows, totals);
    }

    private static Long clientId(Map<String, String> filters) {
        String value = filters == null ? null : filters.get(CLIENT_FILTER);
        if (value == null) {
            return null;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ReportFilterValidationException(
                    "Valor inválido para o filtro " + CLIENT_FILTER + ": " + value + ".");
        }
    }

    private static Map<String, Object> row(TripResultDTO result) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("viagemId", result.tripId());
        row.put("data", result.plannedDepartureOrigin().toLocalDate());
        row.put("trecho", result.route());
        row.put("cliente", result.client());
        row.put("situacao", result.status().getLabel());
        row.put("cobrado", result.charged());
        row.put("recebido", result.received());
        row.put("gastos", result.expenses());
        row.put("resultado", result.result());
        return row;
    }

    private static BigDecimal sum(List<TripResultDTO> results, Function<TripResultDTO, BigDecimal> value) {
        return results.stream()
                .map(value)
                .filter(Objects::nonNull)
                .reduce(TripResultDTO.money(BigDecimal.ZERO), BigDecimal::add);
    }

    private static ReportTotalDTO money(String key, String title, BigDecimal value) {
        return new ReportTotalDTO(key, title, ReportValueFormat.MOEDA, value);
    }
}
